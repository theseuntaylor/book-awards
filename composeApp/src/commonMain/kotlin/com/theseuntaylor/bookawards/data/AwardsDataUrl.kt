package com.theseuntaylor.bookawards.data

const val PUBLISHED_AWARDS_DATA_URL = "https://raw.githubusercontent.com/theseuntaylor/book-awards/main/data/awards.json"

/** Where the app checks for newer data. Android debug builds can point it at a local server for E2E checks. */
expect val awardsDataUrl: String
