/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.renegarcia.phys3proj.helper;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 *
 * @author Rene
 */
public class Test
{
    public static void main(String args[])
    {
        
        try(FileInputStream fis = new FileInputStream("sine861.wav"))
        {
            for(int i = 0; i < 44; i++)
                fis.read();
            
            byte[] ba = new byte[2048];
            fis.read(ba);
            double data[] = new double[1024];
            
            ByteBuffer bb = ByteBuffer.wrap(ba);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            for(int i = 0; i < 1024; i++)
                data[i] = bb.getShort();
            
            
            for(int i = 0; i < 1024; i++)
                data[i] = 0.5*(1-Math.cos(2*Math.PI * i / (1024.0 - 1))) * data[i];
            
            
            DoubleFFT_1D fft = new DoubleFFT_1D(1024);

          
            fft.realForward(data);

            for (int i = 0; i < data.length; i = i + 2)
            {
                System.out.printf("%d,%.0f\n", i / 2, Math.sqrt(data[i] * data[i] + data[i + 1] * data[i + 1]));
            }

            
            
            
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
      
        
        
        
    }
}

