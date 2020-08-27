import { NativeModules } from 'react-native';

const RNSerialPortModule = NativeModules.RNSerialPort;

export default class RNSerialPort {
    static getFilePort(path: string, baudRate: number) {
        return RNSerialPortModule.getFilePort(path, baudRate);
    }

    static getUSBPort(vendorId: number, productId: number, baudRate: number) {
        return RNSerialPortModule.getUSBPort(vendorId, productId, baudRate);
    }
}
