import { NativeEventEmitter, NativeModules } from "react-native";
import { getError } from "./reason-code";

const WiFiP2PManager = NativeModules.WiFiP2PManagerModule;
const WiFiP2PEventEmitter = new NativeEventEmitter(NativeModules.WiFiP2PManagerModule)

// ACTIONS
const PEERS_UPDATED_ACTION = "PEERS_UPDATED";
const CONNECTION_INFO_UPDATED_ACTION = "CONNECTION_INFO_UPDATED";
const THIS_DEVICE_CHANGED_ACTION = "THIS_DEVICE_CHANGED_ACTION";
const DNSTXTRECORD_AVAILABLE_ACTION = "DNSTXTRECORDAVAILABLE";
const DNSSDSERVICE_AVAILABLE_ACTION = "DNSSDSERVICEAVAILABLE";

// CONSTS
const MODULE_NAME = "WIFI_P2P";

const initialize = () => WiFiP2PManager.init();

const startDiscoveringPeers = () =>
  new Promise((resolve, reject) => {
    WiFiP2PManager.discoverPeers((reasonCode) => {
      reasonCode === undefined
        ? resolve("success")
        : reject(getError(reasonCode));
    });
  });

const subscribeOnEvent = (event, callback) => {
    return WiFiP2PEventEmitter.addListener(`${MODULE_NAME}:${event}`, callback);
};

const subscribeOnThisDeviceChanged = (callback) =>
  subscribeOnEvent(THIS_DEVICE_CHANGED_ACTION, callback);

const subscribeOnPeersUpdates = (callback) =>
  subscribeOnEvent(PEERS_UPDATED_ACTION, callback);

const subscribeOnDnsTxtRecordAvailable = (callback) =>
  subscribeOnEvent(DNSTXTRECORD_AVAILABLE_ACTION, callback);

const subscribeOnConnectionInfoUpdates = (callback) => subscribeOnEvent(CONNECTION_INFO_UPDATED_ACTION, callback);

const subscribeOnDnsSdServiceAvailable = (callback) =>
  subscribeOnEvent(DNSSDSERVICE_AVAILABLE_ACTION, callback);

const connect = (deviceAddress) => connectWithConfig({ deviceAddress });

const connectWithConfig = (args) =>
  new Promise((resolve, reject) => {
    WiFiP2PManager.connectWithConfig(args, (status) => {
      status === undefined ? resolve() : reject(getError(status));
    });
  });

const cancelConnect = () =>
  new Promise((resolve, reject) => {
    WiFiP2PManager.cancelConnect((status) => {
      status === undefined ? resolve() : reject(getError(status));
    });
  });

const createGroup = () =>
  new Promise((resolve, reject) => {
    WiFiP2PManager.createGroup((reasonCode) => {
      reasonCode === undefined ? resolve() : reject(getError(reasonCode));
    });
  });

const removeGroup = () =>
  new Promise((resolve, reject) => {
    WiFiP2PManager.removeGroup((reasonCode) => {
      reasonCode === undefined ? resolve() : reject(getError(reasonCode));
    });
  });

const getAvailablePeers = () => WiFiP2PManager.getAvailablePeersList();

const stopDiscoveringPeers = () =>
  new Promise((resolve, reject) => {
    WiFiP2PManager.stopPeerDiscovery((reasonCode) => {
      reasonCode === undefined ? resolve() : reject(getError(reasonCode));
    });
  });

const sendFile = (pathToFile) => WiFiP2PManager.sendFile(pathToFile);

const sendFileTo = (pathToFile, address) => WiFiP2PManager.sendFileTo(pathToFile, address);

const receiveFile = (folder, fileName, forceToScanGallery = false) => new Promise((resolve, reject) => {
    WiFiP2PManager.receiveFile(folder, fileName, forceToScanGallery, (pathToFile) => {
        resolve(pathToFile);
      }
    );
  });

const sendMessage = (message) => WiFiP2PManager.sendMessage(message);

const sendMessageTo = (message, address) => WiFiP2PManager.sendMessageTo(message, address);

const receiveMessage = (props) => new Promise((resolve, reject) => {
    WiFiP2PManager.receiveMessage(props, (message) => {
        resolve(message);
    });
  });

const stopReceivingMessage = () => WiFiP2PManager.stopReceivingMessage()

const getConnectionInfo = () => WiFiP2PManager.getConnectionInfo();

const getGroupInfo = () => WiFiP2PManager.getGroupInfo();

const getPeerList = () => WiFiP2PManager.getPeerList();

const discoverService = () => WiFiP2PManager.discoverService();

const startServiceRegistration = (arg) =>
  WiFiP2PManager.startServiceRegistration(arg);

export {
  // public methods
  initialize,
  discoverService,
  startServiceRegistration,
  startDiscoveringPeers,
  stopDiscoveringPeers,
  subscribeOnThisDeviceChanged,
  subscribeOnPeersUpdates,
  subscribeOnConnectionInfoUpdates,
  subscribeOnDnsTxtRecordAvailable,
  subscribeOnDnsSdServiceAvailable,
  getAvailablePeers,
  connect,
  connectWithConfig,
  cancelConnect,
  createGroup,
  removeGroup,
  getConnectionInfo,
  getGroupInfo,
  getPeerList,
  sendFile,
  sendFileTo,receiveFile,
  sendMessage,
  sendMessageTo,receiveMessage,stopReceivingMessage,
  // system methods
  subscribeOnEvent,
  // const
  PEERS_UPDATED_ACTION,
  CONNECTION_INFO_UPDATED_ACTION,
  THIS_DEVICE_CHANGED_ACTION,
};
