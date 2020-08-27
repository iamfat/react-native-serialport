# @genee/react-native-serialport

## Usage
```typescript
import { RNSerialPort } from '@genee/react-native-serialport';

const port = RNSerialPort.getPort('/dev/ttyS0', 9600);
await port.write();

```