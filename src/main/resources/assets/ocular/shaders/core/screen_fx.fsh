#version 150

uniform sampler2D DiffuseSampler;

uniform float Time;
uniform float BloodIntensity;
uniform float RainIntensity;
uniform float SnowIntensity;
uniform float SandIntensity;
uniform float SandSeed;
uniform float ExplosionIntensity;
uniform float CameraYawDelta;
uniform float CameraPitch;
uniform float StormBlend;
uniform vec2 ScreenSize;

uniform float WaterSplash;
uniform float IsColdBiome;

in vec2 texCoord;
out vec4 fragColor;

vec4 safeSample(vec2 uv) {
    uv = clamp(uv, vec2(0.001), vec2(0.999));
    return texture(DiffuseSampler, uv);
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 N13(float p) {
    vec3 p3 = fract(vec3(p) * vec3(0.1031, 0.11369, 0.13787));
    p3 += dot(p3, p3.yzx + 19.19);
    return fract(vec3((p3.x + p3.y) * p3.z, (p3.x + p3.z) * p3.y, (p3.y + p3.z) * p3.x));
}

float N(float t) {
    return fract(sin(t * 12345.564) * 7658.76);
}

float Saw(float threshold, float t) {
    return smoothstep(0.0, threshold, t) * smoothstep(1.0, threshold, t);
}

// === ЭКСТРЕМАЛЬНАЯ КОНТУЗИЯ ОТ ВЗРЫВА ===
vec3 applyConcussion(vec2 uv, float intensity) {
    if (intensity <= 0.001) return safeSample(uv).rgb;

    vec2 centerCoords = uv - 0.5;
    float aspectRatio = ScreenSize.x / max(ScreenSize.y, 1.0);
    centerCoords.x *= aspectRatio;

    int samples = 15;
    vec3 col = vec3(0.0);
    float wSum = 0.0;

    // Блюр тоже сделали исчезающим чуть быстрее
    float blurAmount = 0.15 * (intensity * intensity * intensity);

    for (int i = 0; i < samples; i++) {
        float t = float(i) / float(samples - 1.0);
        float scale = 1.0 - blurAmount * t;

        // ИСПРАВЛЕНО: Хроматическая аберрация
        // Возводим интенсивность в 4-ю степень. Это значит, что искажение каналов
        // пропадет почти мгновенно (за долю секунды), как только интенсивность начнет падать.
        float fastIntensity = intensity * intensity * intensity * intensity;

        // Уменьшили разлет каналов с 0.08 до 0.03 (красные дуги больше не будут уезжать далеко за края)
        float ca = 0.03 * fastIntensity * t;

        vec2 coordR = centerCoords * (scale + ca);
        vec2 coordG = centerCoords * scale;
        vec2 coordB = centerCoords * (scale - ca);

        coordR.x /= aspectRatio;
        coordG.x /= aspectRatio;
        coordB.x /= aspectRatio;

        col.r += safeSample(coordR + 0.5).r;
        col.g += safeSample(coordG + 0.5).g;
        col.b += safeSample(coordB + 0.5).b;
        wSum += 1.0;
    }
    col /= wSum;

    // Обесцвечивание тоже быстрее возвращается в норму
    float gray = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(gray), clamp((intensity * intensity * intensity) * 2.0, 0.0, 0.8));

    float dist = length(centerCoords);
    // Виньетка (потемнение краев) быстрее ослабевает
    float vigEdge = mix(1.2, 0.3, intensity * intensity);
    float vignette = smoothstep(vigEdge + 0.5, vigEdge, dist);

    return col * vignette;
}

// === ПЕСОК: НАЛИПАЮЩИЕ ТОЧКИ (СТАТИЧНЫЕ) ===
vec2 StaticSandDots(vec2 uv, float scale, float densityThreshold, float seedOffset) {
    vec2 st = uv * scale;

    vec2 id = floor(st);
    vec2 f = fract(st) - 0.5;

    vec3 n = N13(id.x * 12.34 + id.y * 56.78 + seedOffset + SandSeed);

    if (n.y > densityThreshold) return vec2(0.0);

    vec2 pos = (n.xy - 0.5) * 0.7;
    float dist = length(f - pos);
    float radius = mix(0.1, 0.18, n.x);

    float mask = smoothstep(radius, radius * 0.2, dist);

    return vec2(mask, n.z);
}

vec3 applySand(vec3 base, vec2 uv, float intensity, float pitch) {
    if (intensity <= 0.001) return base;

    float aspectRatio = ScreenSize.x / max(ScreenSize.y, 1.0);
    vec2 uvScaled = uv;
    uvScaled.x = (uv.x - 0.5) * aspectRatio + 0.5;

    vec2 center = uv - 0.5;
    center.x *= aspectRatio;
    float distFromCenter = length(center);
    float vignetteMask = smoothstep(0.25, 0.8, distFromCenter);

    float lookDownFactor = mix(0.8, 1.5, smoothstep(-45.0, 90.0, pitch));
    float finalDensity = intensity * lookDownFactor * vignetteMask;

    vec2 layer1 = StaticSandDots(uvScaled, 45.0, finalDensity * 0.5, 1.0);
    vec2 layer2 = StaticSandDots(uvScaled, 65.0, finalDensity * 0.35, 8.0);

    float sandMask = clamp(layer1.x + layer2.x, 0.0, 1.0);

    float randColorVal = layer1.x > 0.0 ? layer1.y : layer2.y;

    vec3 baseSand = vec3(0.85, 0.78, 0.45);
    vec3 finalSandColor = baseSand * mix(0.85, 1.15, randColorVal);
    finalSandColor = clamp(finalSandColor, 0.0, 1.0);

    return mix(base, finalSandColor, sandMask);
}

// === СНЕГ ===
vec2 OrganicClumps(vec2 uv, float time, float scale, float density) {
    vec2 st = uv * scale;
    vec2 id = floor(st);
    vec2 f = fract(st) - 0.5;
    vec3 n = N13(id.x * 23.4 + id.y * 56.7);
    if (n.y > density) return vec2(0.0);
    float life = fract(time * 0.25 + n.z);
    float hitFadeIn = smoothstep(0.0, 0.1, life);
    float meltPhase = smoothstep(0.2, 0.45, life);
    float isFading = smoothstep(0.65, 1.0, life);
    vec2 pos = vec2((n.x - 0.5) * 0.3, (n.y - 0.5) * 0.15 + 0.1);
    float sag = meltPhase * 0.25;
    pos.y -= sag;
    vec2 warpedF = f;
    warpedF.x += sin(f.y * 6.0 + n.z * 10.0) * 0.02;
    warpedF.y += cos(f.x * 6.0 + n.x * 10.0) * 0.02;
    warpedF.y /= (1.0 + sag * 1.2);
    float dist = length(warpedF - pos);
    float baseRadius = mix(0.12, 0.18, n.x);
    float currentRadius = baseRadius * mix(1.0, 1.2, meltPhase);
    float initSoftness = mix(1.0, 0.35, smoothstep(0.0, 0.15, life));
    float edgeSoftness = mix(initSoftness, 0.7 + isFading * 0.3, meltPhase);
    float shape = smoothstep(currentRadius, currentRadius * (1.0 - edgeSoftness), dist);
    shape *= hitFadeIn * (1.0 - isFading);
    float snowMask = shape * (1.0 - meltPhase);
    float waterMask = shape * meltPhase;
    return vec2(snowMask, waterMask);
}

vec3 applySnow(vec3 base, vec2 uv, float intensity) {
    if (intensity <= 0.001) return base;
    float aspectRatio = ScreenSize.x / max(ScreenSize.y, 1.0);
    vec2 uvScaled = uv;
    uvScaled.x = (uv.x - 0.5) * aspectRatio + 0.5;
    vec2 clumpsBig = OrganicClumps(uvScaled, Time, 5.0, 0.15);
    vec2 clumpsSmall = OrganicClumps(uvScaled + vec2(0.6, 0.2), Time * 1.1, 10.0, 0.06);
    float snowMask = clumpsBig.x + clumpsSmall.x;
    float waterMask = clumpsBig.y + clumpsSmall.y;
    vec2 distortDist = vec2(-0.02, -0.03) * waterMask;
    vec3 refractedBase = safeSample(uv + distortDist).rgb;
    float waterHighlight = smoothstep(0.0, 0.5, waterMask) * 0.15;
    vec3 result = mix(base, refractedBase + waterHighlight, clamp(waterMask * 2.0 * intensity, 0.0, 1.0));
    float whiteMask = clamp(snowMask, 0.0, 1.0);
    result = mix(result, vec3(0.95, 0.98, 1.0), whiteMask * intensity);
    return result;
}

// === БЛЮР (ЗАПОТЕВАНИЕ) ===
vec3 applyCondensation(vec2 uv, float mask) {
    if (mask <= 0.001) return safeSample(uv).rgb;
    float aspectRatio = ScreenSize.x / max(ScreenSize.y, 1.0);
    vec3 col = vec3(0.0);
    float radius = 0.015 * mask;
    float h = hash(uv * 200.0);
    float pi = 3.1415926535;
    float wSum = 0.0;
    for (float i = 0.0; i < 8.0; i++) {
        float ang = (i / 8.0) * pi * 2.0 + h * pi;
        vec2 offset = vec2(cos(ang), sin(ang)) * radius;
        offset.x /= aspectRatio;
        col += safeSample(uv + offset).rgb;
        wSum += 1.0;
    }
    col += safeSample(uv).rgb * 2.0;
    wSum += 2.0;
    col /= wSum;
    vec3 frostColor = vec3(0.85, 0.90, 0.95);
    return mix(col, col * frostColor * 1.3, 0.33 * mask);
}

// === ДОЖДЬ И КРОВЬ ===
vec3 applyWaterVignette(vec3 base, vec2 uv, float intensity, float isCold) {
    if (intensity <= 0.001) return base;
    vec2 c = uv - 0.5;
    c.x *= ScreenSize.x / max(ScreenSize.y, 1.0);
    float dist = length(c);
    float mask = smoothstep(0.4, 1.1, dist) * intensity;
    vec2 flowUV = uv;
    flowUV.y += Time * 0.4;
    float n = hash(flowUV * 15.0) * 0.5 + hash(flowUV * 40.0) * 0.5;
    vec2 distort = vec2(n - 0.5) * 0.08 * mask * (1.0 - isCold * 0.5);
    vec3 sampled = safeSample(uv + distort).rgb;
    vec3 warmColor = vec3(0.6, 0.85, 0.95);
    vec3 coldColor = vec3(0.9, 0.95, 1.0);
    vec3 tintColor = mix(warmColor, coldColor, isCold);
    return mix(base, mix(sampled, sampled * tintColor * (1.2 + isCold * 0.3), mask * 0.85), mask);
}

vec3 applyBlood(vec3 base, vec2 uv, float intensity) {
    if (intensity <= 0.001) return base;
    vec2 c = uv - 0.5;
    c.x *= ScreenSize.x / max(ScreenSize.y, 1.0);
    float dist = length(c);
    float vign = smoothstep(0.25, 0.85, dist);
    float n = hash(uv * 80.0) * 0.5 + hash(uv * 320.0) * 0.5;
    float blotch = smoothstep(0.45, 0.95, vign + n * 0.25 * intensity);
    vec3 bloodColor = vec3(0.55, 0.02, 0.03);
    float gray = dot(base, vec3(0.299, 0.587, 0.114));
    vec3 desat = mix(base, vec3(gray), 0.6 * blotch * intensity);
    vec3 result = desat * (1.0 - 0.5 * blotch * intensity);
    result = mix(result, bloodColor, blotch * intensity * 0.55);
    return result * (1.0 + sin(Time * 6.0) * 0.08 * intensity);
}

vec2 DropLayer(vec2 uv, float time, float inertia, float mainDensity, float trailDensity) {
    vec2 initialUV = uv;
    uv.y += time * 0.75;
    vec2 gridSpacing = vec2(6.0, 1.0);
    vec2 grid = gridSpacing * 2.0;
    vec2 gridId = floor(uv * grid);
    float colShift = N(gridId.x);
    uv.y += colShift;
    gridId = floor(uv * grid);
    vec3 noise = N13(gridId.x * 35.2 + gridId.y * 2376.1);
    vec2 stepUV = fract(uv * grid) - vec2(0.5, 0.0);
    float x = noise.x - 0.5;
    float wiggleY = initialUV.y * 20.0;
    float wiggle = sin(wiggleY + sin(wiggleY + time));
    x += wiggle * (0.5 - abs(x)) * (noise.z - 0.5);
    x *= 0.7;
    float ti = fract(time + noise.z);
    float y = (Saw(0.85, ti) - 0.5) * 0.9 + 0.5;
    float tilt = -inertia * 0.3;
    float yDistHead = 1.0 - y;
    float dropOffset = tilt * (yDistHead * yDistHead);
    vec2 dropDelta = stepUV - vec2(x + dropOffset, y);
    dropDelta.x -= (-2.0 * tilt * yDistHead) * dropDelta.y * 0.3;
    float distance = length(dropDelta * gridSpacing.yx);
    float mainDrop = smoothstep(0.4, 0.0, distance);
    float yDistTrail = 1.0 - stepUV.y;
    float isRareStream = step(0.85, fract(noise.y * 831.32));
    float streamWiggle = sin(initialUV.y * 25.0 + noise.z * 10.0) * 0.05 + sin(initialUV.y * 80.0 + noise.x * 10.0) * 0.015;
    float connectPhase = smoothstep(y, y + 0.15, stepUV.y);
    float trailX = (x + tilt * (yDistTrail * yDistTrail)) + streamWiggle * isRareStream * connectPhase;
    float colDistance = abs(stepUV.x - trailX);
    float radius = sqrt(smoothstep(1.0, y, stepUV.y));
    float trailWidth = mix(0.15, 0.40, isRareStream);
    float trail = smoothstep(0.13 * radius, trailWidth * radius * radius, colDistance);
    float trailFront = smoothstep(-0.02, 0.02, stepUV.y - y);
    trail *= trailFront * radius * radius;
    float yOrig = initialUV.y;
    float yMod = fract(yOrig * 10.0) + (stepUV.y - 0.5);
    float yDistTiny = 1.0 - yMod;
    float tinyTrailX = x + tilt * (yDistTiny * yDistTiny) + streamWiggle * isRareStream;
    float droplets = smoothstep(0.3, 0.0, length(stepUV - vec2(tinyTrailX, yMod)));
    float keepTiny = step(fract(floor(initialUV.y * 10.0) * 123.456 + noise.x), trailDensity);
    droplets *= keepTiny * (1.0 - isRareStream);
    float result = mainDrop + droplets * radius * trailFront;
    float keepMain = step(fract(noise.z * 1234.56), mainDensity);
    return vec2(result, trail) * keepMain;
}

float StaticDrops(vec2 uv, float time, float inertia, float density) {
    uv *= 40.0;
    vec2 gridId = floor(uv);
    uv = fract(uv) - vec2(0.5, 0.5);
    vec3 noise = N13(gridId.x * 107.45 + gridId.y * 3543.654);
    vec2 position = (noise.xy - 0.5) * 0.7;
    position.x -= inertia * 0.05;
    float distance = length(uv - position);
    float fade = Saw(0.025, fract(time + noise.z));
    float keepStatic = step(fract(noise.y * 6543.21), density);
    return smoothstep(0.3, 0.0, distance) * fract(noise.z * 10.0) * fade * keepStatic;
}

vec2 Drops(vec2 uv, float time, float l0, float l1, float l2, float inertia, float mDens, float tDens, float sDens) {
    float staticDrops = StaticDrops(uv, time, inertia, sDens) * l0;
    vec2 layer1 = DropLayer(uv, time, inertia, mDens, tDens) * l1;
    vec2 uvSlanted = uv * 1.85;
    uvSlanted.x -= uvSlanted.y * 0.25;
    vec2 layer2 = DropLayer(uvSlanted, time * 1.25, inertia * 0.8, mDens, tDens) * l2;
    float combinedDrops = smoothstep(0.3, 1.0, staticDrops + layer1.x + layer2.x);
    float combinedTrails = max(layer1.y * l0, layer2.y * l1);
    return vec2(combinedDrops, combinedTrails);
}

vec3 applyRain(vec3 base, vec2 uv, float intensity) {
    if (intensity <= 0.001) return base;
    float aspectRatio = ScreenSize.x / max(ScreenSize.y, 1.0);
    vec2 uvScaled = uv;
    uvScaled.x = (uv.x - 0.5) * aspectRatio + 0.5;
    float dropSpeed = Time * 0.35;
    float staticStr = smoothstep(-0.5, 1.0, intensity) * 2.0;
    float layer1 = smoothstep(0.25, 0.75, intensity);
    float layer2 = smoothstep(0.0, 0.5, intensity);
    float mainDens = mix(0.55, 0.35, StormBlend);
    float statDens = mix(0.30, 0.20, StormBlend);
    float trailDens = mix(0.55, 0.35, StormBlend);
    vec2 c = Drops(uvScaled, dropSpeed, staticStr, layer1, layer2, CameraYawDelta, mainDens, trailDens, statDens);
    vec2 e = vec2(0.001, 0.0);
    float cx = Drops(uvScaled + e, dropSpeed, staticStr, layer1, layer2, CameraYawDelta, mainDens, trailDens, statDens).x;
    float cy = Drops(uvScaled + e.yx, dropSpeed, staticStr, layer1, layer2, CameraYawDelta, mainDens, trailDens, statDens).x;
    vec2 n = vec2(cx - c.x, cy - c.x);
    float nLen = length(n);
    vec2 nDir = nLen > 0.00001 ? n / nLen : vec2(0.0);
    vec3 refractedColor = safeSample(uv - nDir * c.x * 0.06).rgb;
    vec3 trailColor = safeSample(uv - nDir * c.y * 0.02).rgb;
    float edgeDark = smoothstep(0.0, 0.3, c.x) - smoothstep(0.3, 0.8, c.x);
    vec3 dropColor = refractedColor * (1.0 - edgeDark * 0.15);
    dropColor = mix(dropColor, vec3(0.2, 0.5, 0.9), 0.02);
    float highlight = smoothstep(0.3, 0.9, -nDir.y * 0.8 - nDir.x * 0.3) * smoothstep(0.2, 0.8, c.x);
    dropColor += vec3(1.0) * highlight * 0.15;
    vec3 result = mix(base, trailColor, clamp(c.y * 0.5, 0.0, 1.0));
    return mix(result, dropColor, clamp(c.x * 0.6, 0.0, 1.0));
}

void main() {
    float shockwaveClean = 1.0 - clamp(ExplosionIntensity * 3.0, 0.0, 1.0);

    float effRain = RainIntensity * shockwaveClean;
    float effSnow = SnowIntensity * shockwaveClean;
    float effSand = SandIntensity * shockwaveClean;
    float effSplash = WaterSplash * shockwaveClean;

    vec3 color = applyConcussion(texCoord, ExplosionIntensity);

    float aspectRatio = ScreenSize.x / max(ScreenSize.y, 1.0);

    color = applyWaterVignette(color, texCoord, effSplash, IsColdBiome);

    float fogIntensity = max(effRain * StormBlend, effSnow * 0.8);
    if (fogIntensity > 0.001) {
        vec2 c = texCoord - 0.5;
        c.x *= aspectRatio;
        float baseFog = smoothstep(0.65, 1.05, length(c)) * 0.9;

        vec2 breathUV = texCoord - vec2(0.5, 0.35);
        breathUV.x *= aspectRatio;
        float breathSpace = smoothstep(0.17, 0.0, length(breathUV));
        float breathFog = breathSpace * smoothstep(0.5, 1.0, sin(Time * 0.5)) * 0.85;

        float totalFogMask = (baseFog + breathFog) * fogIntensity;
        vec3 frostColor = vec3(0.85, 0.90, 0.95);
        color = mix(color, color * frostColor * 1.3, 0.33 * totalFogMask);
    }

    color = applySnow(color, texCoord, effSnow);

    color = applySand(color, texCoord, effSand, CameraPitch);

    if (effRain > 0.001) {
        color = applyRain(color, texCoord, effRain);
    }

    color = applyBlood(color, texCoord, BloodIntensity);

    fragColor = vec4(color, 1.0);
}