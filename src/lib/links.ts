// FrameBeat is one Universal Purchase record on the App Store (Mac + iPad),
// so a single listing URL covers both native apps.
export const APP_STORE_ID = '6811452156'
export const APP_STORE_URL = `https://apps.apple.com/app/id${APP_STORE_ID}`
export const PLAY_STORE_ID = 'io.adambailey.framebeat'
export const PLAY_STORE_URL = `https://play.google.com/store/apps/details?id=${PLAY_STORE_ID}`
// The Android app is in closed testing, where the listing 404s for anyone
// outside the tester list. Flip this once it is published to production.
export const PLAY_STORE_LIVE = false
export const PRIVACY_URL = 'https://framebeat.adambailey.io/privacy'
