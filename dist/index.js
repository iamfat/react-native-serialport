import { NativeEventEmitter, NativeModules } from 'react-native';

const { RNSerialPort } = NativeModules;
class SerialPort {
    constructor(deviceId, baudRate) {
        this.eventListeners = {};
        const emitter = new NativeEventEmitter(RNSerialPort);
        this.nativeSubscription = emitter.addListener(`SerialPort.event@${deviceId}`, (event, ...args) => {
            (this.eventListeners[event] || []).map((f) => f(...args));
        });
        this.deviceId = deviceId;
        this.baudRate = baudRate;
        this.open();
    }
    write(chunk) {
        return RNSerialPort.writePort(this.deviceId, new Uint8Array(chunk));
    }
    close() {
        return RNSerialPort.closePort(this.deviceId);
    }
    open() {
        RNSerialPort.openPort(this.deviceId, this.baudRate);
    }
    destroy() {
        if (this.nativeSubscription) {
            this.nativeSubscription.remove();
            this.nativeSubscription = undefined;
        }
    }
    on(event, cb) {
        this.eventListeners[event] = (this.eventListeners[event] || []).filter((f) => f !== cb);
        this.eventListeners[event].push(cb);
        return this;
    }
    off(event, cb) {
        this.eventListeners[event] = (this.eventListeners[event] || []).filter((f) => f !== cb);
        return this;
    }
}

var index = {
    openPort(deviceId, baudRate = 9600) {
        return new SerialPort(deviceId, baudRate);
    }
};

export default index;
