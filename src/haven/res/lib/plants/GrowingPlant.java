package haven.res.lib.plants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import haven.Config;
import haven.FastMesh.MeshRes;
import haven.Message;
import haven.Resource;
import haven.Sprite;
import haven.Sprite.Factory;
import haven.Sprite.Owner;
import haven.Sprite.ResourceException;
import haven.resutil.CSprite;

public class GrowingPlant implements Factory {
    public final int num;

    public GrowingPlant(int num) {
        this.num = num;
    }

    public Sprite create(Owner owner, Resource res, Message sdt) {
        int stg = sdt.uint8();
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
                Random rnd = owner.mkrandoom();
                for(int i = 0; i < this.num; ++i) {
                    MeshRes mesh = (MeshRes)meshes.get(rnd.nextInt(meshes.size()));
                    if(this.num > 1) {
                        cs.addpart(rnd.nextFloat() * 11.0F - 5.5F, rnd.nextFloat() * 11.0F - 5.5F, mesh.mat.get(), mesh.m);
                    } else {
                        cs.addpart(rnd.nextFloat() * 4.4F - 2.2F, rnd.nextFloat() * 4.4F - 2.2F, mesh.mat.get(), mesh.m);
                    }
                }
            }
            return cs;
        }
    }
}
