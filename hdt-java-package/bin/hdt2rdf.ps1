param(
    [Parameter()]
    [Switch]
    $version,
    [Parameter(ValueFromRemainingArguments, Position = 0)]
    [string[]]
    $OtherParams
)

& "$(Get-Item $PSScriptRoot)/javaenv.ps1" "org.rdfhdt.hdt.tools.HDT2RDF" -RequiredParameters $PSBoundParameters
