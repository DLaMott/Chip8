package sample;

import java.util.Random;

public class Memory {

    private int opcode;
    private int[] memory = new int[4096]; //All mem needed for chip8
    private int[] C = new int[16]; // 15 CPU registers
    private int I; //indexing
    private int pc; // Counter value 0x000 - 0xFFF

    private int[] stack = new int[16]; //Location between jumps
    private int sp; //Stack pointer

    private int delayTimer; //Timer counts at 60HZ
    private int soundTimer; //Sound at zero

    private boolean drawFlag = false;
    private static final Random RANDOM = new Random();

    private Screen screen;
    private Keyboard keyboard;

    public Memory(Screen s, Keyboard k) {
        this.screen = s;
        this.keyboard = k;

        pc = 0x200;
        opcode = 0;
        I = 0;
        sp = 0;

        //Reset memory
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;

        }
        //Rest stack and C registers
        for (int i = 0; i < 16; i++) {
            stack[i] = 0;
            C[i] = 0;
        }

        //Load font
        for (int i = 0; i < 80; i++) {
            memory[i] = Keyboard.FONT[i];
        }

        this.drawFlag = true;
        this.delayTimer = 0;
        this.soundTimer = 0;

    }

    public void loadProgram(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            memory[i + 512] = (b[i] & 0xFF);
        }

    }

    public void fetchOpcode() {
        opcode = ((memory[pc] << 8) | (memory[pc + 1]));
    }

    public void decodeOpcode() {
        int x = 0;

        switch (opcode) {
            case 0x00E0:
                screen.clear();
                drawFlag = true;
                pc += 2;
                return;
            case 0x00EE:
                pc = stack[sp--];
                drawFlag = true;
                pc += 2;
                return;
        }
        switch (opcode & 0xF000) {
            case 0x1000:
                pc = opcode & 0x0FFF;
                return;
            case 0X2000:
                stack[++sp] = pc;

                pc = opcode & 0x0FFF;
                return;
            case 0x3000:
                // 3XNN - skip next inst if Vx = kk.
                if (C[(opcode & 0x0F00) >>> 8] == (opcode & 0x00FF)) {
                    pc += 4;
                } else {
                    pc += 2;
                }

                return;
            case 0x4000:
                // 4XNN - Skip next inst if Vx != kk.
                if (C[(opcode & 0x0F00) >>> 8] != (opcode & 0x00FF)) {
                    pc += 4;
                } else {
                    pc += 2;
                }
                return;
            case 0x5000:
                // 5XY0 - Skip next inst if Vx = Vy.
                if (C[(opcode & 0x0F00) >>> 8] == C[(opcode & 0x00F0) >>> 4]) {
                    pc += 4;
                } else {
                    pc += 2;
                }
                return;
            case 0x6000:
                // 6XNN - Set Vx = kk.
                C[(opcode & 0x0F00) >>> 8] = (opcode & 0x00FF);

                pc += 2;
                return;
            case 0x7000:
                // 7XNN - adds NN to VX.
                x = (opcode & 0x0F00) >>> 8;
                //V[x] = ((V[x] + (opcode & 0x00FF)) & 0xFF);
                int NN = (opcode & 0x00FF);
                int result = C[x] + NN;
                // resolve overflow
                if (result >= 256) {
                    C[x] = result - 256;
                } else {
                    C[x] = result;
                }

                pc += 2;
                return;
        }

        switch (opcode & 0xF00F) {
            case 0x8000:
                // 8XY0 - Set Vx = Vy.
                C[(opcode & 0x0F00) >>> 8] = C[(opcode & 0x00F0) >>> 4];
                pc += 2;
                return;
            case 0x8001:
                // 8XY1 - Set Vx = (Vx OR Vy).
                x = (opcode & 0x0F00) >>> 8;
                C[x] = (C[x] | C[(opcode & 0x00F0) >>> 4]);
                pc += 2;
                return;
            case 0x8002:
                // 8XY2 - Set Vx = (Vx AND Vy).
                x = (opcode & 0x0F00) >>> 8;
                C[x] = (C[x] & C[(opcode & 0x00F0) >>> 4]);
                pc += 2;
                return;
            case 0x8003:
                // 8XY3 - Set Vx = Vx XOR Vy.
                x = (opcode & 0x0F00) >>> 8;
                C[x] = (C[x] ^ C[(opcode & 0x00F0) >>> 4]);

                pc += 2;
                return;
            case 0x8004:
                // 8XY4 - Set Vx = Vx + Vy, set VF = carry.
                x = (opcode & 0x0F00) >>> 8;
                int sum = C[x] + C[(opcode & 0x00F0) >>> 4];

                C[0xF] = sum > 0xFF ? 1 : 0;
                C[x] = (sum & 0xFF);

                pc += 2;
                return;
            case 0x8005:
                // 8XY5 - Set Vx = Vx - Vy, set VF = NOT borrow.
                x = (opcode & 0x0F00) >>> 8;

                if (C[(opcode & 0x00F0) >>> 4] > C[x]) {
                    C[0xF] = 0; //There is a borrow.
                } else {
                    C[0xF] = 1;
                }

                C[x] = (C[x] - C[(opcode & 0x00F0) >>> 4]) & 0xFF;

                pc += 2;
                return;
            case 0x8006:
                // 8XY6 - Set Vx = Vx SHR 1.
                // Shift Vx right by 1. Sets VF to the least significant bit of Vx before shift.
                x = (opcode & 0x0F00) >>> 8;

                C[0xF] = (C[x] & 0x1) == 1 ? 1 : 0;

                C[x] = (C[x] >>> 1);

                pc += 2;
                return;
            case 0x8007:
                // 8XY7 - Set Vx = Vy - Vx, set VF = NOT borrow.
                // VF is set to 0 when there is a borrow and 1 otherwise.
                x = (opcode & 0x0F00) >>> 8;

                if (C[(opcode & 0x00F0) >>> 4] > C[x]) {
                    C[0xF] = 1;
                } else {
                    C[0xF] = 0;
                }

                C[x] = ((C[(opcode & 0x00F0) >>> 4] - C[x]) & 0xFF);

                pc += 2;
                return;
            case 0x800E:
                // 8XYE - Set Vx = Vx SHL 1.
                // Shift Vx left by 1. Sets VF to the value of the most significant bit of Vx before the shift.
                x = (opcode & 0x0F00) >>> 8;

                C[0xF] = (C[x] >>> 7) == 0x1 ? 1 : 0;

                C[x] = ((C[x] << 1) & 0xFF);

                pc += 2;
                return;
            case 0x9000:
                // 9XY0 - Skip next instruction if Vx != Vy.
                x = (opcode & 0x0F00) >>> 8;

                if (C[x] != C[(opcode & 0x00F0) >>> 4]) {
                    pc += 4;
                } else {
                    pc += 2;
                }

                return;
        }

        switch (opcode & 0xF000) {
            case 0xA000:
                // ANNN - Set I = nnn.
                I = (opcode & 0x0FFF);

                pc += 2;
                return;
            case 0xB000:
                // BNNN - Jump to location nnn + V0.
                pc = (opcode & 0x0FFF) + C[0];

                return;
            case 0xC000:
                // CXNN - Set Vx = random byte AND NN.
                x = (opcode & 0x0F00) >>> 8;

                C[x] = ((RANDOM.nextInt(256)) & (opcode & 0x00FF));

                pc += 2;
                return;
            case 0xD000:
                // DXYN - Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision.
                x = C[(opcode & 0x0F00) >> 8];
                int y = C[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;
                C[0xF] = 0;
                for (int yLine = 0; yLine < height; yLine++) {
                    int pixel = memory[I + yLine];

                    for (int xLine = 0; xLine < 8; xLine++) {
                        // check each bit (pixel) in the 8 bit row
                        if ((pixel & (0x80 >> xLine)) != 0) {

                            // wrap pixels if they're drawn off screen
                            int xCoord = x + xLine;
                            int yCoord = y + yLine;

                            if (xCoord < 64 && yCoord < 32) {
                                // if pixel already exists, set carry (collision)
                                if (screen.getPixel(xCoord, yCoord) == 1) {
                                    C[0xF] = 1;
                                }
                                // draw via xor
                                screen.setPixel(xCoord, yCoord);
                            }
                        }
                    }
                }
                drawFlag = true;
                pc += 2;
                return;
        }

        switch (opcode & 0xF0FF) {
            case 0xE09E:
                // EX9E - Skip next instruction if key with the value of Vx is pressed.
                if (keyboard.isPressed(C[(opcode & 0x0F00) >>> 8])) {
                    pc += 4;
                } else {
                    pc += 2;
                }

                return;
            case 0xE0A1:
                // EXA1 - Skip next instruction if key with the value of Vx is not pressed.
                if (!keyboard.isPressed(C[(opcode & 0x0F00) >>> 8])) {
                    pc += 4;
                } else {
                    pc += 2;
                }

                return;
            case 0xF007:
                // FX07 - Set Vx = delay timer value.
                x = (opcode & 0x0F00) >>> 8;
                C[x] = (delayTimer & 0xFF);

                pc += 2;
                return;
            case 0xF00A:
                // FX0A - Wait for a key press, store the value of the key in Vx.
                x = (opcode & 0x0F00) >>> 8;

                for (int j = 0; j <= 0xF; j++) {
                    if (keyboard.isPressed(j)) {
                        C[x] = j;
                        pc += 2;
                        return;
                    }
                }

                //If no key was pressed return, try again.
                return;
            case 0xF015:
                // FX15 - Set delay timer = Vx.
                x = (opcode & 0x0F00) >>> 8;

                this.delayTimer = C[x];

                pc += 2;
                return;
            case 0xF018:
                // FX18 - Set sound timer = Vx.
                x = (opcode & 0x0F00) >>> 8;

                this.soundTimer = C[x];

                pc += 2;
                return;
            case 0xF01E:
                // FX1E - Set I = I + Vx.
                x = (opcode & 0x0F00) >>> 8;

                //Setting VF to 1 when range overflow.
                if (I + C[x] > 0xFFF) {
                    C[0xF] = 1;
                } else {
                    C[0xF] = 0;
                }

                I = ((I + C[x]) & 0xFFF);

                pc += 2;
                return;
            case 0xF029:
                // FX29 - Set I = location of sprite for digit Vx.
                x = (opcode & 0x0F00) >>> 8;

                I = C[x] * 5;
                drawFlag = true;
                pc += 2;
                return;
            case 0xF033:
                // FX33 - Store binary coded decimal representation of Vx
                // in memory locations I, I+1, and I+2.
                x = (opcode & 0x0F00) >>> 8;

                memory[I] = (C[x] / 100);
                memory[I + 1] = ((C[x] % 100) / 10);
                memory[I + 2] = ((C[x] % 100) % 10);

                pc += 2;
                return;
            case 0xF055:
                // FX55 - Store registers V0 through Vx in memory starting at location I.
                x = (opcode & 0x0F00) >>> 8;

                for (int j = 0; j <= x; j++) {
                    memory[I + j] = C[j];
                }

                pc += 2;
                return;
            case 0xF065:
                // FX65 - Read registers V0 through Vx from memory starting at location I.
                x = (opcode & 0x0F00) >>> 8;

                for (int j = 0; j <= x; j++) {
                    C[j] = memory[I + j] & 0xFF;
                }

                pc += 2;
                return;
        }
    }

    // getter and setters for sound and flags
    public int getDelayTimer() {
        return this.delayTimer;
    }

    public void setDelayTimer(int d) {
        this.delayTimer = d;
    }

    public void setSoundTimer(int s) {
        this.soundTimer = s;
    }

    public int getSoundTimer() {
        return this.soundTimer;
    }

    public boolean isDrawFlag() {
        return this.drawFlag;
    }

    public void setDrawFlag(boolean b) {
        this.drawFlag = b;

    }
}

