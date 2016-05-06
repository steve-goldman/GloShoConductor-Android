#include <jni.h>
#include <stdint.h>

JNIEXPORT void JNICALL
Java_constantbeta_com_gloshoconductor_imageprocessors_ImageProcessorThresholded_encode__Ljava_nio_ByteBuffer_2Ljava_nio_ByteBuffer_2II(
        JNIEnv *env, jobject instance, jobject src, jobject dest, jint size, jint threshold) {

    const uint8_t OFF       = 0;
    const uint8_t ON        = 255;

    uint8_t *srcArray  = (*env)->GetDirectBufferAddress(env, src);
    uint8_t *destArray = (*env)->GetDirectBufferAddress(env, dest);

    int i;
    for (i = 0; i < size; i++)
    {
        if (srcArray[i] >= threshold)
        {
            destArray[i] = ON;
        }
        else
        {
            destArray[i] = OFF;
        }
    }
}
