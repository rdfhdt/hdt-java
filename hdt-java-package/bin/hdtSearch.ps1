param(
    [Parameter()]
    [Switch]
    $version,
    [Parameter()]
    [Switch]
    $memory,
    [Parameter(ValueFromRemainingArguments, Position = 0)]
    [string[]]
    $OtherParams
)

& "$(Get-Item $PSScriptRoot)/javaenv.ps1" "org.rdfhdt.hdt.tools.HdtSearch" -RequiredParameters $PSBoundParameters
