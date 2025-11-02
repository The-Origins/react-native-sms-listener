declare module 'react-native-sms-listener' {
  /**
   * Requests SMS permissions on Android.
   */
  export function requestSmsPermissions(): Promise<boolean>

  /**
   * Starts listening for SMS messages from a specific contact.
   * Messages that match the given regex are stored as strings.
   */
  export function startCapture(
    contactName: string,
    storeCondition: string
  ): Promise<void>

  /**
   * Stops SMS capture.
   */
  export function stopCapture(): Promise<void>

  /**
   * Checks whether SMS capture is currently active.
   */
  export function isCaptureActive(): Promise<boolean>

  /**
   * Fetches stored SMS message bodies captured while app was inactive.
   */
  export function getStoredReceipts(): Promise<string[]>

  /**
   * Clears stored SMS messages from the native cache.
   */
  export function clearStoredReceipts(): Promise<boolean>

  /**
   * delete a single message with id:number
   */
  export function deleteReceipt(id: number): Promise<boolean>

  /**
   * Add sms capture subsription
   */
  export function addOnMessageCapturedListener(callback: (r: {id:number, body:string}) => void): {
    remove: () => void
  }

  /**
   * Remove sms capture subsription
   */
  export function removeMessageCapturedListener(): void

  const SmsReceiptListener: {
    requestSmsPermissions: typeof requestSmsPermissions
    startCapture: typeof startCapture
    stopCapture: typeof stopCapture
    isCaptureActive: typeof isCaptureActive
    getStoredReceipts: typeof getStoredReceipts
    clearStoredReceipts: typeof clearStoredReceipts
  }

  export default SmsReceiptListener
}