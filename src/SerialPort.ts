import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';

const { RNSerialPort } = NativeModules;

type SerialPortEvent = 'open' | 'close' | 'error' | 'data';

export default class SerialPort {
    private deviceId: string;
    private baudRate: number;
    private eventListeners: { [event: string]: Function[] } = {};
    private nativeSubscription: EmitterSubscription;

    constructor(deviceId: string, baudRate: number) {
        const emitter = new NativeEventEmitter(RNSerialPort);
        this.nativeSubscription = emitter.addListener(
            `SerialPort.event@${deviceId}`,
            (event: SerialPortEvent, ...args) => {
                (this.eventListeners[event] || []).map((f) => f(...args));
            },
        );
        this.deviceId = deviceId;
        this.baudRate = baudRate;
        this.open();
    }

    write(chunk: ArrayBuffer): Promise<number> {
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

    on(event: SerialPortEvent, cb: Function) {
        this.eventListeners[event] = (this.eventListeners[event] || []).filter((f) => f !== cb);
        this.eventListeners[event].push(cb);
        return this;
    }

    off(event: SerialPortEvent, cb: Function) {
        this.eventListeners[event] = (this.eventListeners[event] || []).filter((f) => f !== cb);
        return this;
    }
}
