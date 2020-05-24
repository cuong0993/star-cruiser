@file:JsQualifier("THREE")

package three.loaders

import three.objects.Group

external class GLTFLoader(
    manager: LoadingManager? = definedExternally
) : Loader {

    fun load(
        url: String,
        onLoad: (GLTF) -> Unit = definedExternally,
        onProgress: ((dynamic) -> Unit)? = definedExternally,
        onError: ((dynamic) -> Unit)? = definedExternally
    )
}

external interface GLTF {
    var scene: Group
}
