# Generates icon.png (window icon) and multi-resolution icon.ico (installer)
# Dark rounded square, cyan "A" monogram with an amber accent triangle.
Add-Type -AssemblyName System.Drawing

$outDir = $PSScriptRoot
$sizes = 16, 32, 48, 64, 128, 256

function New-IconBitmap([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    # Rounded dark background
    $radius = [Math]::Max(2, [int]($size * 0.18))
    $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $radius * 2
    $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
    $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
    $path.CloseFigure()

    $bgBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.Color]::FromArgb(255, 16, 26, 30),
        [System.Drawing.Color]::FromArgb(255, 10, 16, 18),
        [System.Drawing.Drawing2D.LinearGradientMode]::Vertical)
    $g.FillPath($bgBrush, $path)

    # Amber accent triangle (bottom-right, like a mountain/tooth)
    $t = $size * 0.06
    $tri = @(
        (New-Object System.Drawing.PointF(($size * 0.58), ($size * 0.82))),
        (New-Object System.Drawing.PointF(($size * 0.78), ($size * 0.52))),
        (New-Object System.Drawing.PointF(($size * 0.95), ($size * 0.82)))
    )
    $amber = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230, 255, 183, 77))
    $g.FillPolygon($amber, $tri)

    # Cyan "A"
    $fontSize = [Math]::Max(6, [single]($size * 0.62))
    $font = New-Object System.Drawing.Font("Segoe UI", $fontSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $cyan = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 77, 208, 225))
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Center
    $textRect = New-Object System.Drawing.RectangleF(0, ($size * 0.02), ($size * 0.9), $size)
    $g.DrawString("A", $font, $cyan, $textRect, $fmt)

    $g.Dispose()
    return $bmp
}

# PNG for the window icon (256px)
$png256 = New-IconBitmap 256
$png256.Save((Join-Path $outDir "..\src\main\resources\icon.png"), [System.Drawing.Imaging.ImageFormat]::Png)

# Multi-resolution ICO with PNG-compressed entries
$pngBlobs = @()
foreach ($s in $sizes) {
    $bmp = New-IconBitmap $s
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngBlobs += , @($s, $ms.ToArray())
    $bmp.Dispose()
}

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

"icon.png and icon.ico generated"
