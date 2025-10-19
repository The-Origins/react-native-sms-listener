import { NativeModules, Platform, PermissionsAndroid } from "react-native";

const { SmsListenerModule } = NativeModules;

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

export default {
  requestSmsPermissions,
  startCapture,
  stopCapture,
  isCaptureActive,
  getStoredReceipts,
  clearStoredReceipts,
};
