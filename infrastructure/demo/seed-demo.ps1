$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8088"
$envFile = Join-Path $PSScriptRoot "..\oracle\.env"

$username = "demo-user"
$password = "DemoPassword123!"
$email = "demo-user@banking-demo.local"

$checkingIban = "ES9100491500051234567891"
$savingsIban = "ES6800491500059876543210"

Write-Host ""
Write-Host "=== Banking Platform demo seed ==="
Write-Host ""

if (-not (Test-Path $envFile)) {
    throw "Environment file not found: $envFile"
}

$internalToken = Get-Content $envFile |
    Where-Object { $_ -like "INTERNAL_SERVICE_TOKEN=*" } |
    Select-Object -First 1 |
    ForEach-Object { $_.Substring("INTERNAL_SERVICE_TOKEN=".Length) }

if ([string]::IsNullOrWhiteSpace($internalToken)) {
    throw "INTERNAL_SERVICE_TOKEN was not found in $envFile"
}

Write-Host "1. Creating demo identity user..."

$userBody = @{
    username = $username
    email = $email
    password = $password
} | ConvertTo-Json

$user = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/identity-api/api/v1/users" `
    -ContentType "application/json" `
    -Body $userBody

$identityUserId = $user.id

Write-Host "   Identity user created: $identityUserId"

Write-Host "2. Activating demo identity user..."

$sql = @"
UPDATE USERS
SET STATUS = 'ACTIVE'
WHERE ID = HEXTORAW(REPLACE('$identityUserId', '-', ''));

COMMIT;

EXIT;
"@

$sql | docker exec -i banking-oracle-demo bash -lc 'sqlplus -s "BANK_IDENTITY/${BANK_IDENTITY_PASSWORD}@localhost:1521/FREEPDB1"'

Write-Host "   Identity user activated."

Write-Host "3. Logging in..."

$loginBody = @{
    username = $username
    password = $password
} | ConvertTo-Json

$login = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/identity-api/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

$token = $login.accessToken

if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login succeeded but no accessToken was returned."
}

Write-Host "   Login successful."

Write-Host "4. Creating customer..."

$customerBody = @{
    identityUserId = $identityUserId
    firstName = "Demo"
    lastName = "User"
    documentType = "DNI"
    documentNumber = "12345678Z"
    dateOfBirth = "1990-01-01"
    phoneNumber = "600000000"
    fiscalAddress = "Calle Demo 1"
    postalCode = "48001"
    city = "Bilbao"
    countryCode = "ES"
} | ConvertTo-Json

$customer = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/customer-api/api/v1/customers" `
    -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $token" } `
    -Body $customerBody

$customerId = $customer.id

Write-Host "   Customer created: $customerId"

Write-Host "5. Creating checking account..."

$checkingBody = @{
    customerId = $customerId
    iban = $checkingIban
    accountType = "CHECKING"
    currency = "EUR"
} | ConvertTo-Json

$checkingAccount = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/account-api/api/accounts" `
    -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $token" } `
    -Body $checkingBody

$checkingAccountId = $checkingAccount.id

Write-Host "   Checking account created: $checkingAccountId"

Write-Host "6. Creating savings account..."

$savingsBody = @{
    customerId = $customerId
    iban = $savingsIban
    accountType = "SAVINGS"
    currency = "EUR"
} | ConvertTo-Json

$savingsAccount = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/account-api/api/accounts" `
    -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $token" } `
    -Body $savingsBody

$savingsAccountId = $savingsAccount.id

Write-Host "   Savings account created: $savingsAccountId"

Write-Host "7. Crediting checking account with 1000 EUR..."

$creditBody = @{
    amount = 1000.00
} | ConvertTo-Json

$creditOperationId = [guid]::NewGuid().ToString()

Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/account-api/api/internal/accounts/$checkingAccountId/credit" `
    -ContentType "application/json" `
    -Headers @{
        "X-Internal-Service-Token" = $internalToken
        "Idempotency-Key" = $creditOperationId
    } `
    -Body $creditBody | Out-Null

Write-Host "   Initial balance credited."

Write-Host "8. Creating example transfer of 150 EUR..."

$transferBody = @{
    sourceAccountId = $checkingAccountId
    destinationIban = $savingsIban
    amount = 150.00
    currency = "EUR"
    idempotencyKey = [guid]::NewGuid().ToString()
} | ConvertTo-Json

$transfer = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/transfer-api/api/transfers" `
    -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $token" } `
    -Body $transferBody

Write-Host "   Transfer created: $($transfer.id)"
Write-Host "   Transfer status: $($transfer.status)"

Write-Host ""
Write-Host "=== Demo seed completed successfully ==="
Write-Host ""
Write-Host "Login:"
Write-Host "  Username: $username"
Write-Host "  Password: $password"
Write-Host ""
Write-Host "Accounts:"
Write-Host "  Checking: $checkingIban"
Write-Host "  Savings:  $savingsIban"
Write-Host ""
Write-Host "Expected balances:"
Write-Host "  Checking: 850 EUR"
Write-Host "  Savings:  150 EUR"
Write-Host ""