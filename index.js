import { NativeModules, Platform } from 'react-native'

const { SmsReceiptModule } = NativeModules

function ensureAndroid() {
  if (Platform.OS !== 'android') {
    throw new Error('react-native-sms-receipt-listener: android only')
  }
  if (!SmsReceiptModule) {
    throw new Error('SmsReceiptModule native module not found — did you install & rebuild the native app?')
  }
}

/**
 * startCapture(contactName: string): Promise<boolean>
 * - contactName: free text used to match sender or body (case-insensitive substring)
 */
export async function startCapture(contactName) {
  ensureAndroid()
  if (typeof contactName !== 'string' || contactName.trim().length === 0) {
    throw new Error('startCapture requires a non-empty contactName string')
  }
  return await SmsReceiptModule.startCapture(contactName)
}

/**
 * stopCapture(): Promise<boolean>
 * - disables capture and clears target
 */
export async function stopCapture() {
  ensureAndroid()
  return await SmsReceiptModule.stopCapture()
}

/**
 * Optional convenience:
 * isCaptureActive(): Promise<{ enabled: boolean, contact: string | null }>
 */
export async function isCaptureActive() {
  ensureAndroid()
  if (SmsReceiptModule.isCaptureActive) {
    return await SmsReceiptModule.isCaptureActive()
  }
  return { enabled: false, contact: null }
}

/**
 * getStoredReceipts(): Promise<Receipt[]>
 * - Fetches receipts captured while app was closed or in background.
 */
export async function getStoredReceipts() {
  ensureAndroid()
  if (!SmsReceiptModule.getStoredReceipts)
    throw new Error('getStoredReceipts() not implemented on native side')
  return await SmsReceiptModule.getStoredReceipts()
}

/**
 * clearStoredReceipts(): Promise<boolean>
 * - Clears the native cache after syncing to expo-sqlite or backend.
 */
export async function clearStoredReceipts() {
  ensureAndroid()
  if (!SmsReceiptModule.clearStoredReceipts)
    throw new Error('clearStoredReceipts() not implemented on native side')
  return await SmsReceiptModule.clearStoredReceipts()
}

export default {
  startCapture,
  stopCapture,
  isCaptureActive,
  getStoredReceipts,
  clearStoredReceipts,
}