import SerialPort from "./SerialPort";

export default { 
    openPort(deviceId: string, baudRate: number = 9600) {
        return new SerialPort(deviceId, baudRate);
    }
 };
