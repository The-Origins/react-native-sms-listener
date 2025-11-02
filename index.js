import {
  NativeModules,
  Platform,
  PermissionsAndroid,
  NativeEventEmitter,
} from "react-native";

const { SmsListenerModule } = NativeModules;
const emitter = SmsListenerModule
  ? new NativeEventEmitter(SmsListenerModule)
  : null;
let subscription = null;

function ensureAndroid() {
  if (Platform.OS !== "android")
    throw new Error("react-native-sms-listener works only on Android");
}

/**
 * Requests Android SMS-related permissions.
 */
export async function requestSmsPermissions() {
  if (Platform.OS !== "android") return true;
  const perms = [PermissionsAndroid.PERMISSIONS.RECEIVE_SMS];
  const result = await PermissionsAndroid.requestMultiple(perms);
  return perms.every((p) => result[p] === PermissionsAndroid.RESULTS.GRANTED);
}

/**
 * Subscribe to live capture events.
 * callback receives object { id: number, body: string, }
 * Returns a subscription object. Call subscription.remove() to unsubscribe.
 */
export function addOnMessageCapturedListener(callback) {
  ensureAndroid();
  if (!emitter) throw new Error("NativeEventEmitter unavailable");
  //clear previous listeners
  removeMessageCapturedListener()

  subscription = emitter.addListener("SmsReceiptCaptured", callback);
  return subscription;
}

export function removeMessageCapturedListener() {
  if (subscription) {
    subscription.remove();
    subscription = null;
  }
}

/**
 * Starts SMS capture for a specific contact and regex condition.
 */
export async function startCapture(contactName, storeCondition) {
  ensureAndroid();
  return SmsListenerModule.startCapture(contactName, storeCondition);
}

/**
 * Stops SMS capture.
 */
export async function stopCapture() {
  ensureAndroid();
  return SmsListenerModule.stopCapture();
}

/**
 * Checks if SMS capture is currently active.
 */
export async function isCaptureActive() {
  ensureAndroid();
  return SmsListenerModule.isCaptureActive();
}

/**
 * Returns stored SMS messages matching conditions.
 * @returns {Promise<string[]>}
 */
export async function getStoredReceipts() {
  ensureAndroid();
  return SmsListenerModule.getStoredReceipts();
}

/**
 * Clears stored messages.
 */
export async function clearStoredReceipts() {
  ensureAndroid();
  return SmsListenerModule.clearStoredReceipts();
}

/**
 * Delete by id:
 */
export async function deleteReceipt(id) {
  ensureAndroid();
  if (typeof id !== "number") throw new Error("id must be a number");
  return await SmsListenerModule.deleteReceipt(id);
}

export default {
  requestSmsPermissions,
  addOnMessageCapturedListener,
  startCapture,
  stopCapture,
  isCaptureActive,
  getStoredReceipts,
  clearStoredReceipts,
  deleteReceipt,
};
