export default class RNSerialPort {
    static getFilePort(path: string, baudRate: number): any;
    static getUSBPort(vendorId: number, productId: number, baudRate: number): any;
}
