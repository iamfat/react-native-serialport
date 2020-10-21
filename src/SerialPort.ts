import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';

const { RNSerialPort } = NativeModules;
const emitter = new NativeEventEmitter(RNSerialPort);

type SerialPortEvent = 'open' | 'close' | 'error' | 'data';

export default class SerialPort {
    private deviceId: string;
    private baudRate: number;
    private eventListeners: { [event: string]: Function[] } = {};
    private nativeSubscription: EmitterSubscription;

    constructor(deviceId: string, baudRate: number) {
        this.nativeSubscription = emitter.addListener(`SerialPort.event@${deviceId}`, ({ event, params }) => {
            if (event === 'data' && Array.isArray(params[0])) {
                params[0] = new Uint8Array(params[0]).buffer;
            }
            (this.eventListeners[event] || []).map((f) => f(...(params || [])));
        });
        this.deviceId = deviceId;
        this.baudRate = baudRate;
        this.open();
    }

    write(chunk: ArrayBuffer): Promise<number> {
        return RNSerialPort.writePort(this.deviceId, Array.from(new Uint8Array(chunk)));
    }

    close() {
        RNSerialPort.closePort(this.deviceId);
        if (this.nativeSubscription) {
            this.nativeSubscription.remove();
            this.nativeSubscription = undefined;
        }
    }

    open() {
        RNSerialPort.openPort(this.deviceId, this.baudRate);
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
