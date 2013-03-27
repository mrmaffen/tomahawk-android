/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk.network.test;

import junit.framework.Assert;

import org.tomahawk.libtomahawk.network.Msg;
import org.tomahawk.tomahawk_android.test.TomahawkTestData;

import android.test.AndroidTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MsgTest extends AndroidTestCase {

    private Msg nullMsg = null;

    private Msg emptyMsg = null;

    private Msg fullMsg = null;

    public void setUp() {

        nullMsg = new Msg();
        emptyMsg = new Msg(TomahawkTestData.tstFirstMsg().length(), Msg.JSON);
        fullMsg = new Msg(TomahawkTestData.tstFirstMsg(), Msg.JSON);
    }

    /**
     * Simple unit test for outgoing writes.
     */
    public void testWriteMsg() {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            fullMsg.write(out);
        } catch (IOException e) {
            e.printStackTrace();

            /** unit test failure. */
            Assert.assertTrue(false);
        }

        byte[] receivedBody = Msg.bodyFromTwkPacket(out.toString().getBytes());
        Assert.assertTrue(new String(receivedBody).equals(fullMsg.getPayload()));
    }

    public void testMsgLength() {
        int length = TomahawkTestData.tstFirstMsg().length();

        Assert.assertTrue(nullMsg.length() != length);
        Assert.assertTrue(emptyMsg.length() == length);
        Assert.assertTrue(fullMsg.length() == length);
    }

    public void testBegin() {

        /** Testing with fully populated Msg. */
        ByteBuffer buf = ByteBuffer.allocate(Msg.headerSize());
        buf.putInt(fullMsg.length());
        buf.put(fullMsg.getFlags());

        Msg msg = Msg.begin(buf.array());

        Assert.assertTrue(msg.length() == fullMsg.length());
        Assert.assertTrue(msg.getFlags() == fullMsg.getFlags());

        /** Testing with empty Msg which has header info. */
        buf = ByteBuffer.allocate(Msg.headerSize());
        buf.putInt(emptyMsg.length());
        buf.put(emptyMsg.getFlags());

        msg = Msg.begin(buf.array());

        Assert.assertTrue(msg.length() == emptyMsg.length());
        Assert.assertTrue(msg.getFlags() == emptyMsg.getFlags());
    }

    public void testMsgSizeFromTwkPacket() {
        String payload = TomahawkTestData.tstFirstMsg();

        byte[] packet = TomahawkTestData.getTstTwkPacket(payload);

        int size = Msg.msgSizeFromTwkPacket(packet);
        int otherSize = payload.length();

        Assert.assertTrue(size == otherSize);
    }

    public void testFlagFromTwkPacket() {
        String payload = TomahawkTestData.tstFirstMsg();

        byte[] packet = TomahawkTestData.getTstTwkPacket(payload);

        int flag = Msg.flagFromTwkPacket(packet);
        int otherFlag = Msg.JSON;

        Assert.assertTrue(flag == otherFlag);
    }

    public void testBodyFromTwkPacket() {
        String payload = TomahawkTestData.tstFirstMsg();

        byte[] packet = TomahawkTestData.getTstTwkPacket(payload);

        String otherPayload = new String(Msg.bodyFromTwkPacket(packet));
        Assert.assertTrue(payload.equals(otherPayload));
    }
}
