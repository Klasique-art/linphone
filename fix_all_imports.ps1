# Fix package imports for safotel - Enhanced Version
# Run this from the project root directory

Write-Host "==================================================" -ForegroundColor Green
Write-Host "  Fixing ALL package imports for safotel" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""

$totalFixed = 0

# Fix R imports in Kotlin files
Write-Host "[1/4] Fixing 'import org.linphone.R' -> 'import com.safotel.app.R'" -ForegroundColor Yellow
$count = 0
Get-ChildItem -Path "app\src" -Recurse -Include *.kt | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -and $content -match 'import org\.linphone\.R\b') {
        $newContent = $content -replace 'import org\.linphone\.R\b', 'import com.safotel.app.R'
        Set-Content $_.FullName -Value $newContent -NoNewline
        Write-Host "  ✓ Fixed: $($_.Name)" -ForegroundColor Cyan
        $count++
    }
}
Write-Host "  Fixed $count files" -ForegroundColor Green
$totalFixed += $count
Write-Host ""

# Fix BuildConfig imports in Kotlin files
Write-Host "[2/4] Fixing 'import com.safotel.app.BuildConfig' -> 'import com.safotel.app.BuildConfig'" -ForegroundColor Yellow
$count = 0
Get-ChildItem -Path "app\src" -Recurse -Include *.kt | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -and $content -match 'import org\.linphone\.BuildConfig') {
        $newContent = $content -replace 'import org\.linphone\.BuildConfig', 'import com.safotel.app.BuildConfig'
        Set-Content $_.FullName -Value $newContent -NoNewline
        Write-Host "  ✓ Fixed: $($_.Name)" -ForegroundColor Cyan
        $count++
    }
}
Write-Host "  Fixed $count files" -ForegroundColor Green
$totalFixed += $count
Write-Host ""

# Fix databinding imports in Kotlin files
Write-Host "[3/4] Fixing 'import com.safotel.app.databinding' -> 'import com.safotel.app.databinding'" -ForegroundColor Yellow
$count = 0
Get-ChildItem -Path "app\src" -Recurse -Include *.kt | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -and $content -match 'import org\.linphone\.databinding') {
        $newContent = $content -replace 'import org\.linphone\.databinding', 'import com.safotel.app.databinding'
        Set-Content $_.FullName -Value $newContent -NoNewline
        Write-Host "  ✓ Fixed: $($_.Name)" -ForegroundColor Cyan
        $count++
    }
}
Write-Host "  Fixed $count files" -ForegroundColor Green
$totalFixed += $count
Write-Host ""

# Fix XML data binding references
Write-Host "[4/4] Fixing 'type=`"org.linphone' -> 'type=`"com.safotel.app' in XML files" -ForegroundColor Yellow
$count = 0
Get-ChildItem -Path "app\src\main\res" -Recurse -Include *.xml -ErrorAction SilentlyContinue | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -and $content -match 'type="org\.linphone') {
        $newContent = $content -replace 'type="org\.linphone', 'type="org.linphone'
        Set-Content $_.FullName -Value $newContent -NoNewline
        Write-Host "  ✓ Fixed: $($_.Name)" -ForegroundColor Cyan
        $count++
    }
}
Write-Host "  Fixed $count files" -ForegroundColor Green
$totalFixed += $count
Write-Host ""

Write-Host "==================================================" -ForegroundColor Green
Write-Host "  TOTAL: Fixed $totalFixed files!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Run: gradlew.bat clean" -ForegroundColor White
Write-Host "  2. Run: gradlew.bat assembleDebug" -ForegroundColor White
Write-Host ""