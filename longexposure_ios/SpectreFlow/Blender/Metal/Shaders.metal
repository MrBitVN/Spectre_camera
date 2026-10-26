#include <metal_stdlib>
using namespace metal;

// Mean Accumulation Kernel
kernel void meanAccumulateKernel(
    texture2d<float, access::read> currentFrame [[texture(0)]],
    device float4* accumulatorBuffer [[buffer(0)]],
    uint2 gid [[thread_position_in_grid]]
) {
    if (gid.x >= currentFrame.get_width() || gid.y >= currentFrame.get_height()) {
        return;
    }

    uint index = gid.y * currentFrame.get_width() + gid.x;
    float4 pixel = currentFrame.read(gid);

    accumulatorBuffer[index] += pixel;
}

// Mean Finalize Kernel
kernel void meanFinalizeKernel(
    const device float4* accumulatorBuffer [[buffer(0)]],
    texture2d<float, access::write> outputTexture [[texture(0)]],
    constant uint& frameCount [[buffer(1)]],
    uint2 gid [[thread_position_in_grid]]
) {
    if (gid.x >= outputTexture.get_width() || gid.y >= outputTexture.get_height()) {
        return;
    }

    uint index = gid.y * outputTexture.get_width() + gid.x;
    float4 sum = accumulatorBuffer[index];
    float4 meanColor = sum / float(frameCount);
    meanColor.a = 1.0;

    outputTexture.write(meanColor, gid);
}

// Light Trails Max Kernel
kernel void lightTrailsAccumulateKernel(
    texture2d<float, access::read> currentFrame [[texture(0)]],
    texture2d<float, access::read> previousAcc [[texture(1)]],
    texture2d<float, access::write> outputTexture [[texture(2)]],
    uint2 gid [[thread_position_in_grid]]
) {
    if (gid.x >= currentFrame.get_width() || gid.y >= currentFrame.get_height()) {
        return;
    }

    float4 current = currentFrame.read(gid);
    float4 prev = previousAcc.read(gid);

    float4 maxColor = float4(
        max(current.r, prev.r),
        max(current.g, prev.g),
        max(current.b, prev.b),
        1.0
    );

    outputTexture.write(maxColor, gid);
}
