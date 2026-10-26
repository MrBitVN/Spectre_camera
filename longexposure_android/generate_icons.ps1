Add-Type -AssemblyName System.Drawing

function Draw-SpectreFlowIcon([int]$size, [string]$outputPath, [bool]$rounded = $true) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    # Background
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 12, 14, 16))
    if ($rounded) {
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $radius = [float]($size * 0.22)
        $diameter = $radius * 2
        $path.AddArc(0, 0, $diameter, $diameter, 180, 90)
        $path.AddArc($size - $diameter, 0, $diameter, $diameter, 270, 90)
        $path.AddArc($size - $diameter, $size - $diameter, $diameter, $diameter, 0, 90)
        $path.AddArc(0, $size - $diameter, $diameter, $diameter, 90, 90)
        $path.CloseFigure()
        $g.FillPath($bgBrush, $path)

        $borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(60, 46, 56, 66), [float]($size * 0.015))
        $g.DrawPath($borderPen, $path)
        $borderPen.Dispose()
        $path.Dispose()
    } else {
        $g.FillRectangle($bgBrush, 0, 0, $size, $size)
    }

    $cx = [float]($size / 2.0)
    $cy = [float]($size / 2.0)

    # Outer lens aperture ring
    $lensRadius = [float]($size * 0.36)
    $lensPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(100, 0, 230, 118), [float]($size * 0.025))
    $g.DrawEllipse($lensPen, $cx - $lensRadius, $cy - $lensRadius, $lensRadius * 2, $lensRadius * 2)
    $lensPen.Dispose()

    # Flowing Light Crescent Trail
    $crescentPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $crescentPath.AddArc($cx - [float]($size * 0.28), $cy - [float]($size * 0.30), [float]($size * 0.58), [float]($size * 0.60), 220, 190)
    $crescentPath.AddArc($cx - [float]($size * 0.20), $cy - [float]($size * 0.25), [float]($size * 0.44), [float]($size * 0.50), 50, -190)
    $crescentPath.CloseFigure()

    $glowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 0, 230, 118))
    $g.FillPath($glowBrush, $crescentPath)
    $glowBrush.Dispose()
    $crescentPath.Dispose()

    # Inner mint highlight streak
    $innerPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $innerPath.AddArc($cx - [float]($size * 0.24), $cy - [float]($size * 0.26), [float]($size * 0.50), [float]($size * 0.52), 235, 150)
    $innerPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(200, 105, 240, 174), [float]($size * 0.035))
    $g.DrawPath($innerPen, $innerPath)
    $innerPen.Dispose()
    $innerPath.Dispose()

    # Center glowing photon core
    $haloBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(70, 0, 230, 118))
    $haloRadius = [float]($size * 0.12)
    $g.FillEllipse($haloBrush, $cx - $haloRadius, $cy - $haloRadius, $haloRadius * 2, $haloRadius * 2)
    $haloBrush.Dispose()

    $coreBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
    $coreRadius = [float]($size * 0.05)
    $g.FillEllipse($coreBrush, $cx - $coreRadius, $cy - $coreRadius, $coreRadius * 2, $coreRadius * 2)
    $coreBrush.Dispose()

    $bgBrush.Dispose()
    $g.Dispose()
    $bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Android Mipmaps
Draw-SpectreFlowIcon 48 "longexposure_android\app\src\main\res\mipmap-mdpi\ic_launcher.png"
Draw-SpectreFlowIcon 72 "longexposure_android\app\src\main\res\mipmap-hdpi\ic_launcher.png"
Draw-SpectreFlowIcon 96 "longexposure_android\app\src\main\res\mipmap-xhdpi\ic_launcher.png"
Draw-SpectreFlowIcon 144 "longexposure_android\app\src\main\res\mipmap-xxhdpi\ic_launcher.png"
Draw-SpectreFlowIcon 192 "longexposure_android\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png"

# Round icons
Draw-SpectreFlowIcon 48 "longexposure_android\app\src\main\res\mipmap-mdpi\ic_launcher_round.png"
Draw-SpectreFlowIcon 72 "longexposure_android\app\src\main\res\mipmap-hdpi\ic_launcher_round.png"
Draw-SpectreFlowIcon 96 "longexposure_android\app\src\main\res\mipmap-xhdpi\ic_launcher_round.png"
Draw-SpectreFlowIcon 144 "longexposure_android\app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png"
Draw-SpectreFlowIcon 192 "longexposure_android\app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png"

# High-Res store icons
Draw-SpectreFlowIcon 512 "longexposure_android\app\src\main\res\drawable\ic_launcher_512.png"
Draw-SpectreFlowIcon 1024 "longexposure_ios\SpectreFlow\Assets.xcassets\AppIcon.appiconset\AppIcon-1024.png" $false
Draw-SpectreFlowIcon 1024 "docs\logo.png"

Write-Output "App icon generation completed successfully!"
