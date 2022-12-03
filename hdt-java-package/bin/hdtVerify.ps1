param(
    [Parameter()]
    [Switch]
    $unicode,
    [Parameter()]
    [Switch]
    $progress,
    [Parameter()]
    [Switch]
    $color,
    [Parameter()]
    [Switch]
    $binary,
    [Parameter()]
    [Switch]
    $quiet,
    [Parameter()]
    [Switch]
    $load,
    [Parameter()]
    [Switch]
    $equals,
    [Parameter(ValueFromRemainingArguments, Position = 0)]
    [string[]]
    $OtherParams
)

& "$(Get-Item $PSScriptRoot)/javaenv.ps1" "org.rdfhdt.hdt.tools.HDTVerify" -RequiredParameters $PSBoundParameters
