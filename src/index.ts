import SerialPort from './SerialPort';

const openPort = (deviceId: string, baudRate: number = 9600) => new SerialPort(deviceId, baudRate);
export default { openPort, SerialPort };
export { openPort, SerialPort };
