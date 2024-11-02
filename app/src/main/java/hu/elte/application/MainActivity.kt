package hu.elte.application

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import dagger.hilt.android.AndroidEntryPoint
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.ViewNode
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    lateinit var sceneView: ARSceneView
    lateinit var loadingView: View
    lateinit var instructionText: TextView

    val viewModel by viewModels<MainActivityVM>()


    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    var anchorNode: AnchorNode? = null
        set(value) {
            if (field != value) {
                field = value
                updateInstructions()
            }
        }

    var trackingFailureReason: TrackingFailureReason? = null
        set(value) {
            if (field != value) {
                field = value
                updateInstructions()
            }
        }

    fun updateInstructions() {
//        instructionText.text = trackingFailureReason?.let {
//            it.getDescription(this)
//        } ?: if (anchorNode == null) {
//            getString(R.string.point_your_phone_down)
//        } else {
//            null
//        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        loadingView = findViewById<FrameLayout>(R.id.loading_view)
        instructionText = findViewById<TextView>(R.id.instruction_text)

        sceneView = findViewById<ARSceneView?>(R.id.scene_view).apply {

            lifecycle = this@MainActivity.lifecycle
            planeRenderer.isEnabled = true

            configureSession { session, config ->
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            }

            onSessionUpdated = { _, frame ->
                if (anchorNode == null) {
                    frame.getUpdatedPlanes()
                        .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                        ?.let { plane ->
                            plane.createAnchor(plane.centerPose)

                            addAnchorNode(plane.createAnchor(plane.centerPose),"models/kitchen_3d_model.glb")

                            val secondAnchorPose = plane.centerPose.compose(Pose.makeTranslation(0.2f, 0f, 0f))
                            //addAnchorNode(plane.createAnchor(secondAnchorPose), "models/ibm_5110.glb")

                            //loadModels()
                        }

                }
            }
            onTrackingFailureChanged = { reason ->
                this@MainActivity.trackingFailureReason = reason
            }
        }


        sceneView.onTouchEvent = { motionEvent, hitResult ->


            if (motionEvent.action == MotionEvent.ACTION_UP){

                showInformation("this equipment uses for measuring")

            }

            true



        }







    }

    fun loadModels(){

        val model1 = sceneView.modelLoader.createModelInstance(
            "models/kitchen_3d_model.glb"
        ).let { modelInstance ->
            ModelNode(

                modelInstance = modelInstance,
                // Scale to fit in a 0.5 meters cube
                scaleToUnits = 0.5f,
                // Bottom origin instead of center so the model base is on floor
               // centerOrigin = Position(y = -0.5f)
            ).apply {
                isEditable = true

            }
        }

        val model2 = sceneView.modelLoader.createModelInstance(
            "models/ibm_5110.glb"
        ).let { modelInstance ->
            ModelNode(

                modelInstance = modelInstance,
                // Scale to fit in a 0.5 meters cube
                scaleToUnits = 0.5f,
                // Bottom origin instead of center so the model base is on floor
                //centerOrigin = Position(y = -0.5f)
            ).apply {
                isEditable = true
            }
        }

        model1.position =  Position(0f, 0f, 0f)
        model2.position =  Position(0.5f, 0f, 0f)

        model1.addChildNode(model2)

        sceneView.addChildNode(model1)

        sceneView.planeRenderer.isVisible = true  // Show detected planes

        sceneView.onTouchEvent = { motionEvent, hitResult ->


            true



        }





    }





    fun addAnchorNode(anchor: Anchor) {

        sceneView.addChildNode(
            AnchorNode(sceneView.engine, anchor)
                .apply {
                    isEditable = true

                    anchorNode = this@apply

                    lifecycleScope.launch {
                        val attachmentManager =
                            ViewAttachmentManager(this@MainActivity, sceneView)
                        attachmentManager.onResume()
                        val node = ViewNode(sceneView.engine, sceneView.modelLoader, attachmentManager)

                        node.loadView(this@MainActivity, R.layout.main_activity, onLoaded = { instance, view ->
                            anchorNode?.addChildNode(node)

                        })


                    }



                })
    }

    private fun addAnchorNode(anchor: Anchor, modelPath: String) {


        sceneView.addChildNode(
            AnchorNode(sceneView.engine, anchor).apply {

                isEditable = true
                lifecycleScope.launch {
                    isLoading = true

                    buildModelNode(modelPath).let {

                        addChildNode(it)

                    }
                    isLoading = false
                }
                anchorNode = this
            }
        )

    }

    suspend fun buildModelNode(modelPath: String): ModelNode {
        sceneView.modelLoader.createModelInstance(
            modelPath
        ).let { modelInstance ->
            return ModelNode(

                modelInstance = modelInstance,
                // Scale to fit in a 0.5 meters cube
                scaleToUnits = 0.5f,
                // Bottom origin instead of center so the model base is on floor
                centerOrigin = Position(y = -0.5f)
            ).apply {
                isEditable = true
            }
        }

    }


    private fun showInformation(message: String) {

        AlertDialog.Builder(this)
            .create().apply {
                setMessage(message)
                setTitle("Info")
            }.show()



        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }




}
