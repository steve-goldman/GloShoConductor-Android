#include "../../../../../../Library/Android/sdk/ndk-bundle/platforms/android-23/arch-mips64/usr/include/sys/cdefs.h"
#include <jni.h>
#include <stdint.h>

const uint8_t OFF = 0;
const uint8_t ON  = 255;

uint8_t tresholded_value(uint8_t src, jint threshold)
{
    if (src >= threshold)
    {
        return ON;
    }
    return OFF;
}

JNIEXPORT void JNICALL
Java_constantbeta_com_gloshoconductor_imageprocessors_ImageProcessorNative_encodeThresholded(
        JNIEnv *env,
        jclass __unused type,
        jobject src,
        jobject dest,
        jint size,
        jint threshold)
{

    uint8_t *srcArray  = (*env)->GetDirectBufferAddress(env, src);
    uint8_t *destArray = (*env)->GetDirectBufferAddress(env, dest);

    int i;
    for (i = 0; i < size; i++)
    {
        destArray[i] = tresholded_value(srcArray[i], threshold);
    }
}

// turns on the which'th bit if the which'th byte equals or exceeds the
// threshold
uint8_t thresholded_byte_as_bit(uint64_t src, int which, jint threshold)
{
    const int     shift   = 8 * (which - 1);
    const uint8_t mask    = (uint8_t)(1 << (which - 1));
    const uint8_t srcByte = (uint8_t)(src >> shift);

    return tresholded_value(srcByte, threshold) & mask;
}

JNIEXPORT void JNICALL
Java_constantbeta_com_gloshoconductor_imageprocessors_ImageProcessorNative_encodeThresholdedBits(
        JNIEnv *env,
        jclass __unused type,
        jobject src,
        jobject dest,
        jint size,
        jint threshold)
{
    uint64_t *srcArray  = (*env)->GetDirectBufferAddress(env, src);
    uint8_t  *destArray = (*env)->GetDirectBufferAddress(env, dest);

    int i;
    for (i = 0; i < size / 8; i++)
    {
        const uint64_t srcBytes = srcArray[i];
        destArray[i] =
                thresholded_byte_as_bit(srcBytes, 1, threshold) |
                thresholded_byte_as_bit(srcBytes, 2, threshold) |
                thresholded_byte_as_bit(srcBytes, 3, threshold) |
                thresholded_byte_as_bit(srcBytes, 4, threshold) |
                thresholded_byte_as_bit(srcBytes, 5, threshold) |
                thresholded_byte_as_bit(srcBytes, 6, threshold) |
                thresholded_byte_as_bit(srcBytes, 7, threshold) |
                thresholded_byte_as_bit(srcBytes, 8, threshold);
    }
}

int encodeStopBit(int i, uint8_t *array, int offset)
{
    while (i > 127)
    {
        array[offset++] = (uint8_t)(0x80 | (i & 0x7F));
        i >>= 7;
    }
    array[offset++] = (uint8_t)i;
    return offset;
}

JNIEXPORT jint JNICALL
Java_constantbeta_com_gloshoconductor_imageprocessors_ImageProcessorNative_encodeThresholdedDeltaDistances(
        JNIEnv *env,
        jclass __unused type,
        jobject src,
        jobject dest,
        jint size,
        jint threshold)
{
    uint8_t *srcArray  = (*env)->GetDirectBufferAddress(env, src);
    uint8_t *destArray = (*env)->GetDirectBufferAddress(env, dest);

    uint8_t lastState  = OFF;
    int     count      = 0;
    int     totalCount = 0;

    int i;
    for (i = 0; i < size; i++)
    {
        const uint8_t srcByte = srcArray[i];
        if (lastState == OFF && srcByte >= threshold)
        {
            totalCount = encodeStopBit(count, destArray, totalCount);
            lastState = ON;
            count = 1;
        }
        else if (lastState == ON && srcByte < threshold)
        {
            totalCount = encodeStopBit(count, destArray, totalCount);
            lastState = OFF;
            count = 1;
        }
        else
        {
            count++;
        }
    }
    totalCount = encodeStopBit(count, destArray, totalCount);

    return totalCount;
}
