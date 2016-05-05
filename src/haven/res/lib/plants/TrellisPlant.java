package haven.res.lib.plants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import haven.Config;
import haven.FastMesh.MeshRes;
import haven.Gob;
import haven.Message;
import haven.Resource;
import haven.Sprite;
import haven.Sprite.Factory;
import haven.Sprite.Owner;
import haven.Sprite.ResourceException;
import haven.resutil.CSprite;

public class TrellisPlant implements Factory {
    public final int num;

    public TrellisPlant(int num) {
        this.num = num;
    }

    public TrellisPlant() {
        this(2);
    }

    public Sprite create(Owner owner, Resource res, Message std) {
        int stg = std.uint8();
        ArrayList<MeshRes> meshes = new ArrayList<MeshRes>();
        Iterator<MeshRes> allmeshes = res.layers(MeshRes.class).iterator();

        while(allmeshes.hasNext()) {
            MeshRes mesh = (MeshRes)allmeshes.next();
            if(mesh.id / 10 == stg) {
                meshes.add(mesh);
            }
        }

        if(meshes.size() < 1) {
            throw new ResourceException("No variants for grow stage " + stg, res);
        } else {
            CSprite cs = new CSprite(owner, res);
            if (Config.simplecrops) {
                MeshRes mesh = (MeshRes)meshes.get(0);
                cs.addpart(0, 0, mesh.mat.get(), mesh.m);
            } else {
                double var4 = -((Gob)owner).a;
                float var6 = (float)Math.cos(var4);
                float var7 = -((float)Math.sin(var4));
                Random var16 = owner.mkrandoom();
                float var12 = 11.0F / (float)this.num;
                float var13 = -5.5F + var12 / 2.0F;

                for(int var14 = 0; var14 < this.num; ++var14) {
                    MeshRes mesh = (MeshRes)meshes.get(var16.nextInt(meshes.size()));
                    cs.addpart(var13 * var7, var13 * var6, mesh.mat.get(), mesh.m);
                    var13 += var12;
                }
            }

            return cs;
        }
    }
}
