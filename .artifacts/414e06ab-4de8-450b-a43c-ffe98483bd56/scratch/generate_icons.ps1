Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\andi-\Downloads\Gemini_Generated_Image_t6oz1rt6oz1rt6oz.jfif"
if (!(Test-Path $srcPath)) {
    Write-Error "Source image not found at $srcPath"
    exit 1
}

$img = [System.Drawing.Image]::FromFile($srcPath)
Write-Host "Source Size: $($img.Width) x $($img.Height)"

$bgColor = [System.Drawing.Color]::FromArgb(24, 28, 36)
try {
    $bmpSample = New-Object System.Drawing.Bitmap $img
    $bgColor = $bmpSample.GetPixel(0, 0)
    $bmpSample.Dispose()
} catch {}
Write-Host "Background color sampled: $bgColor"

$size = 512
$targetBmp = New-Object System.Drawing.Bitmap $size, $size
$graphics = [System.Drawing.Graphics]::FromImage($targetBmp)
$graphics.Clear($bgColor)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

$w = $img.Width
$h = $img.Height
$scale = [Math]::Min($size / $w, $size / $h)
$newW = [int]($w * $scale)
$newH = [int]($h * $scale)
$x = [int](($size - $newW) / 2)
$y = [int](($size - $newH) / 2)

$graphics.DrawImage($img, $x, $y, $newW, $newH)
$graphics.Dispose()

$drawableDir = "C:\Users\andi-\AndroidStudioProjects\Budget2\app\src\main\res\drawable"
if (!(Test-Path $drawableDir)) { New-Item -ItemType Directory -Force -Path $drawableDir }
$targetBmp.Save("$drawableDir\ic_app_logo.png", [System.Drawing.Imaging.ImageFormat]::Png)

$densities = @(
    @{ Name = "mdpi"; Size = 48 },
    @{ Name = "hdpi"; Size = 72 },
    @{ Name = "xhdpi"; Size = 96 },
    @{ Name = "xxhdpi"; Size = 144 },
    @{ Name = "xxxhdpi"; Size = 192 }
)

foreach ($d in $densities) {
    $dir = "C:\Users\andi-\AndroidStudioProjects\Budget2\app\src\main\res\mipmap-$($d.Name)"
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir }

    $iconBmp = New-Object System.Drawing.Bitmap $d.Size, $d.Size
    $g = [System.Drawing.Graphics]::FromImage($iconBmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.DrawImage($targetBmp, 0, 0, $d.Size, $d.Size)
    $g.Dispose()

    $iconBmp.Save("$dir\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $iconBmp.Save("$dir\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $iconBmp.Dispose()
}

$img.Dispose()
$targetBmp.Dispose()
Write-Host "Icons generated successfully!"
