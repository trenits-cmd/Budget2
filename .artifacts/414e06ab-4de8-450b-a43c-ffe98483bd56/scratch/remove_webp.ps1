$densities = @("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
foreach ($d in $densities) {
    $path = "C:\Users\andi-\AndroidStudioProjects\Budget2\app\src\main\res\mipmap-$d\ic_launcher.webp"
    if (Test-Path $path) {
        Remove-Item -Force $path
        Write-Host "Removed $path"
    }
    $pathRound = "C:\Users\andi-\AndroidStudioProjects\Budget2\app\src\main\res\mipmap-$d\ic_launcher_round.webp"
    if (Test-Path $pathRound) {
        Remove-Item -Force $pathRound
        Write-Host "Removed $pathRound"
    }
}
Write-Host "Done removing webps."
