package cn.wode490390.nukkit.flyingsquid.util;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;

public class LevelReader {

    private Level level;

    public void setLevel(Level level) {
        this.level = level;
    }

    public Block getNormalBlockIfLoaded(int x, int y, int z) {
        if (y >= 0 && y < 256) {
            BaseFullChunk chunk = this.level.getChunkIfLoaded(x >> 4, z >> 4);
            if (chunk != null) {
                Block block = Block.fullList[chunk.getFullBlock(x & 0xf, y, z & 0xf) & 0xfff].clone();
                block.x = x;
                block.y = y;
                block.z = z;
                block.level = this.level;
                return block;
            }
        }

        return null;
    }

    public boolean isNormalAir(int x, int y, int z) {
        Block block = this.getNormalBlockIfLoaded(x, y, z);
        return block != null && block.getId() == Block.AIR;
    }
}
