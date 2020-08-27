import { NativeModules } from 'react-native';

const RNSerialPortModule = NativeModules.RNSerialPort;
class RNSerialPort {
    static getFilePort(path, baudRate) {
        return RNSerialPortModule.getFilePort(path, baudRate);
    }
    static getUSBPort(vendorId, productId, baudRate) {
        return RNSerialPortModule.getUSBPort(vendorId, productId, baudRate);
    }
}

export { RNSerialPort };
