# Generates icon.png (window icon) and multi-resolution icon.ico (installer)
# from packaging/logo.png (golden turtle logo, transparent background).
Add-Type -AssemblyName System.Drawing

$outDir = $PSScriptRoot
$logoPath = Join-Path $outDir "logo.png"
if (-not (Test-Path $logoPath)) { throw "packaging/logo.png not found" }
$sizes = 16, 32, 48, 64, 128, 256

$src = [System.Drawing.Bitmap]::FromFile($logoPath)

function New-IconBitmap([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $scale = [Math]::Min($size / $src.Width, $size / $src.Height)
    $w = [int][Math]::Round($src.Width * $scale)
    $h = [int][Math]::Round($src.Height * $scale)
    $x = [int](($size - $w) / 2)
    $y = [int](($size - $h) / 2)
    $g.DrawImage($src, $x, $y, $w, $h)
    $g.Dispose()
    return $bmp
}

# PNG for the window icon (256px)
$png256 = New-IconBitmap 256
$png256.Save((Join-Path $outDir "..\src\main\resources\icon.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$png256.Dispose()

# Multi-resolution ICO with PNG-compressed entries
$pngBlobs = @()
foreach ($s in $sizes) {
    $bmp = New-IconBitmap $s
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngBlobs += , @($s, $ms.ToArray())
    $bmp.Dispose()
}
$src.Dispose()

$icoPath = Join-Path $outDir "icon.ico"
$fs = [System.IO.File]::Create($icoPath)
$w = New-Object System.IO.BinaryWriter($fs)
$w.Write([UInt16]0)                # reserved
$w.Write([UInt16]1)                # type: icon
$w.Write([UInt16]$pngBlobs.Count)  # image count
$offset = 6 + 16 * $pngBlobs.Count
foreach ($entry in $pngBlobs) {
    $s = $entry[0]; $data = $entry[1]
    $w.Write([Byte]($(if ($s -ge 256) { 0 } else { $s })))  # width (0 = 256)
    $w.Write([Byte]($(if ($s -ge 256) { 0 } else { $s })))  # height
    $w.Write([Byte]0)              # palette
    $w.Write([Byte]0)              # reserved
    $w.Write([UInt16]1)            # planes
    $w.Write([UInt16]32)           # bpp
    $w.Write([UInt32]$data.Length) # data size
    $w.Write([UInt32]$offset)      # data offset
    $offset += $data.Length
}
foreach ($entry in $pngBlobs) { $w.Write($entry[1]) }
$w.Close()

"icon.png and icon.ico generated from logo.png"
