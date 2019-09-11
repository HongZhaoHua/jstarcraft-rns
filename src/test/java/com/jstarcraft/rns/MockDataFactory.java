package com.jstarcraft.rns;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.core.utility.StringUtility;

/**
 * 模拟数据工厂
 * 
 * @author Birdy
 *
 */
public class MockDataFactory {

    private int userSize = 1000;

    private int itemSize = 1000;

    private int profileSize = 1000;

    private float profileScope = 1F;

    private float scoreScope = 5F;

    private int instantSize = 1000;

    private int locationSize = 180;

    private int commentSize = 1000;

    private float commentScope = 1F;

    private float ratio = 0.01F;

    /**
     * user(离散:1:稠密)-profile(连续:n:稀疏)
     * 
     * <pre>
     * 可以当作user(离散:1:稠密)-user(离散:1:稠密)-degree(连续:1:稠密)
     * </pre>
     * 
     * >
     */
    @Test
    public void mockUserProfile() throws Exception {
        File file = new File("data/mock/user-profile");
        FileUtils.deleteQuietly(file);
        file.getParentFile().mkdirs();
        file.createNewFile();
        StringBuilder buffer = new StringBuilder();
        try (FileWriter writer = new FileWriter(file); BufferedWriter out = new BufferedWriter(writer);) {
            for (int leftIndex = 0; leftIndex < userSize; leftIndex++) {
                buffer.setLength(0);
                for (int rightIndex = 0; rightIndex < profileSize; rightIndex++) {
                    if (RandomUtility.randomFloat(1F) < ratio) {
                        float degree = RandomUtility.randomFloat(profileScope);
                        buffer.append(degree);
                    }
                    buffer.append(" ");
                }
                String profile = buffer.substring(0, buffer.length() - 1);
                out.write(StringUtility.format("{} {}", leftIndex, profile));
                out.newLine();
            }
        }
    }

    /**
     * item(离散:1:稠密)-profile(连续:n:稀疏)
     * 
     * <pre>
     * 可以当作item(离散:1:稠密)-item(离散:1:稠密)-degree(连续:1:稠密)
     * </pre>
     */
    @Test
    public void mockItemProfile() throws Exception {
        File file = new File("data/mock/item-profile");
        FileUtils.deleteQuietly(file);
        file.getParentFile().mkdirs();
        file.createNewFile();
        StringBuilder buffer = new StringBuilder();
        try (FileWriter writer = new FileWriter(file); BufferedWriter out = new BufferedWriter(writer);) {
            for (int leftIndex = 0; leftIndex < itemSize; leftIndex++) {
                buffer.setLength(0);
                for (int rightIndex = 0; rightIndex < profileSize; rightIndex++) {
                    if (RandomUtility.randomFloat(1F) < ratio) {
                        float degree = RandomUtility.randomFloat(profileScope);
                        buffer.append(degree);
                    }
                    buffer.append(" ");
                }
                String profile = buffer.substring(0, buffer.length() - 1);
                out.write(StringUtility.format("{} {}", leftIndex, profile));
                out.newLine();
            }
        }
    }

    /**
     * user(离散:1:稠密)-item(离散:1:稠密)-score(连续:1:稠密)-instant(离散:1:稠密)-location(离散:2:稠密)-comment(连续:n:稀疏)
     */
    @Test
    public void mockUserItemScoreInstantLocationComment() throws Exception {
        File file = new File("data/mock/user-item-score-instant-location-comment");
        FileUtils.deleteQuietly(file);
        file.getParentFile().mkdirs();
        file.createNewFile();
        StringBuilder buffer = new StringBuilder();
        try (FileWriter writer = new FileWriter(file); BufferedWriter out = new BufferedWriter(writer);) {
            for (int leftIndex = 0; leftIndex < userSize; leftIndex++) {
                for (int rightIndex = 0; rightIndex < itemSize; rightIndex++) {
                    // 此处故意选择特定的数据(TODO 考虑改为利用正态分布)
                    if (rightIndex < 10 || RandomUtility.randomFloat(1F) < ratio) {
                        // 得分
                        float score = RandomUtility.randomFloat(scoreScope);
                        // 时间
                        int instant = RandomUtility.randomInteger(instantSize);
                        // 地点(经度)
                        int longitude = RandomUtility.randomInteger(locationSize);
                        // 地点(纬度)
                        int latitude = RandomUtility.randomInteger(locationSize);
                        buffer.setLength(0);
                        for (int commentIndex = 0; commentIndex < commentSize; commentIndex++) {
                            if (RandomUtility.randomFloat(1F) < ratio) {
                                float degree = RandomUtility.randomFloat(commentScope);
                                buffer.append(degree);
                            }
                            buffer.append(" ");
                        }
                        // 评论
                        String comment = buffer.substring(0, buffer.length() - 1);
                        out.write(StringUtility.format("{} {} {} {} {} {} {}", leftIndex, rightIndex, score, instant, longitude, latitude, comment));
                        out.newLine();
                    }

                }
            }
        }
    }

}
