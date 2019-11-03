package com.rockbite.tools.talos.editor.addons.bvb;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.rockbite.tools.talos.editor.data.PropertyProviderCenter;
import com.rockbite.tools.talos.editor.wrappers.MutableProperty;
import com.rockbite.tools.talos.editor.wrappers.Property;
import com.rockbite.tools.talos.editor.wrappers.IPropertyProvider;
import com.rockbite.tools.talos.editor.wrappers.ImmutableProperty;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.rockbite.tools.talos.TalosMain;
import com.rockbite.tools.talos.runtime.ParticleEffectDescriptor;
import com.rockbite.tools.talos.runtime.ParticleEffectInstance;
import com.rockbite.tools.talos.runtime.ScopePayload;
import com.rockbite.tools.talos.runtime.values.NumericalValue;

public class BoundEffect implements Json.Serializable, IPropertyProvider  {

    /**
     * parent skeleton container
     */
    SkeletonContainer parent;

    /**
     * name of this effect
     */
    String name;

    /**
     * even though this is one effect, many instances of it can be rendered at the same time
     * in cases when it starts more often then finishes
     */
    private Array<ParticleEffectInstance> particleEffects;

    /**
     * Particle effect descriptor that knows how to spawn the instances
     */
    private ParticleEffectDescriptor particleEffectDescriptor;

    /**
     * positional attachments to the bones
     */
    private Array<AttachmentPoint> valueAttachments;
    private AttachmentPoint positionAttachment;
    /**
     * is it rendered behind animation or in front
     */
    private boolean isBehind;

    /**
     * if true this spawns only once at remains forever no matter what
     */
    private boolean forever = false;

    /**
     * each effect hsa it's own instance of scope payload, we want this global values local to effect type
     */
    private ScopePayload scopePayload;

    /**
     * Start and complete event names, if empty then in sync with skeleton's animation loop
     */
    private String startEvent = "";

    private String completeEvent = "";

    /**
     * System vars
     */
    Vector2 tmpVec = new Vector2();
    NumericalValue val = new NumericalValue();

    public BoundEffect() {
        scopePayload = new ScopePayload();
        particleEffects = new Array<>();
        valueAttachments = new Array<>();
    }

    public BoundEffect(String name, ParticleEffectDescriptor descriptor, SkeletonContainer container) {
        parent = container;
        this.name = name;
        this.particleEffectDescriptor = descriptor;
        scopePayload = new ScopePayload();
        particleEffects = new Array<>();
        valueAttachments = new Array<>();
    }

    public void setForever(boolean isForever) {
        if(isForever && !forever) {
            particleEffects.clear();
            ParticleEffectInstance instance = spawnEffect();
            instance.loopable = true; // this is evil
        }
        forever = isForever;
    }

    private ParticleEffectInstance spawnEffect() {
        ParticleEffectInstance instance = particleEffectDescriptor.createEffectInstance();
        instance.setScope(scopePayload);
        particleEffects.add(instance);

        return instance;
    }

    public void update(float delta) {
        // value attachments
        for(AttachmentPoint attachmentPoint: valueAttachments) {
            if(attachmentPoint.isStatic()) {
                scopePayload.setDynamicValue(attachmentPoint.getSlotId(), attachmentPoint.getStaticValue());
            } else {
                float rotation = parent.getBoneRotation(attachmentPoint.getBoneName());
                tmpVec.set(parent.getBonePosX(attachmentPoint.getBoneName()), parent.getBonePosY(attachmentPoint.getBoneName()));
                tmpVec.add(attachmentPoint.getOffsetX(), attachmentPoint.getOffsetY());

                if (attachmentPoint.getAttachmentType() == AttachmentPoint.AttachmentType.POSITION) {
                    val.set(tmpVec.x, tmpVec.y);
                } else if (attachmentPoint.getAttachmentType() == AttachmentPoint.AttachmentType.ROTATION) {
                    val.set(rotation);
                }
            }

            scopePayload.setDynamicValue(attachmentPoint.getSlotId(), val);
        }

        // update position for each instance and update effect itself
        for(ParticleEffectInstance instance: particleEffects) {
            if (positionAttachment != null) {
                if(positionAttachment.isStatic()) {
                    instance.setPosition(positionAttachment.getStaticValue().get(0), positionAttachment.getStaticValue().get(1));
                } else {
                    instance.setPosition(parent.getBonePosX(positionAttachment.getBoneName()) + positionAttachment.getOffsetX(), parent.getBonePosY(positionAttachment.getBoneName()) + positionAttachment.getOffsetY());
                }

                instance.update(delta);
            }
        }
    }

    public void setBehind(boolean isBehind) {
        this.isBehind = isBehind;
    }

    public boolean isBehind() {
        return isBehind;
    }

    public void removePositionAttachment() {
        positionAttachment = null;
    }

    public void setPositionAttachment(String bone) {
        positionAttachment = new AttachmentPoint();
        positionAttachment.setTypeAttached(bone, -1);
    }

    public void startInstance() {
        if(forever) return;

        ParticleEffectInstance instance = particleEffectDescriptor.createEffectInstance();
        instance.setScope(scopePayload);
        particleEffects.add(instance);
    }

    public void completeInstance() {
        if(forever) return;

        for(ParticleEffectInstance instance: particleEffects) {
            instance.allowCompletion();
        }
    }

    public Array<ParticleEffectInstance> getParticleEffects() {
        return particleEffects;
    }

    @Override
    public Array<Property> getListOfProperties () {
        Array<Property> properties = new Array<>();

        Property<String> boneName = new ImmutableProperty<String>("Bone Name", positionAttachment.getBoneName()) {
            @Override
            public String getValue () {
                return positionAttachment.getBoneName();
            }
        };
        Property<Float> offsetX = new MutableProperty<Float>("Offset X", positionAttachment.getOffsetX()) {
            @Override
            public Float getValue () {
                return positionAttachment.getOffsetX();
            }

            @Override
            public void changed (Float newValue) {
                positionAttachment.setOffsetX(newValue);
            }
        };
        Property<Float> offsetY = new MutableProperty<Float>("Offset Y", positionAttachment.getOffsetY()) {
            @Override
            public Float getValue () {
                return positionAttachment.getOffsetY();
            }

            @Override
            public void changed (Float newValue) {
                positionAttachment.setOffsetY(newValue);
            }
        };
        Property<Boolean> behind = new MutableProperty<Boolean>("Is Behind", isBehind) {
            @Override
            public Boolean getValue () {
                return isBehind;
            }

            @Override
            public void changed (Boolean newValue) {
                isBehind = newValue;
            }
        };

		Property<Array<AttachmentPoint>> globalValues = new MutableProperty<Array<AttachmentPoint>>("", valueAttachments) {
            @Override
            public Array<AttachmentPoint> getValue () {
                return valueAttachments;
            }

            @Override
			public void changed (Array<AttachmentPoint> newValue) {
                valueAttachments.clear();
                valueAttachments.addAll(newValue);
			}
		};
		globalValues.addAdditionalProperty("boneNames", parent.getSkeleton().getBones());

        properties.add(boneName, offsetX, offsetY, behind);
        properties.add(globalValues);
        return properties;
    }

    @Override
    public String getTitle () {
        return "Bound Effect Properties";
    }

    public AttachmentPoint getPositionAttachment() {
        return positionAttachment;
    }

    public Array<AttachmentPoint> getAttachments() {
        return valueAttachments;
    }

    public void updateEffect(ParticleEffectDescriptor descriptor) {
        particleEffectDescriptor = descriptor;
        if(forever) {
            particleEffects.clear();
            ParticleEffectInstance instance = spawnEffect();
            instance.loopable = true; // this is evil
        }
        // else this will get auto-spawned on next event call anyway.
    }

    public void setParent(SkeletonContainer parent) {
        this.parent = parent;
    }

    @Override
    public void write(Json json) {
        json.writeValue("effectName", name);
        json.writeValue("isBehind", isBehind);
        json.writeValue("positionAttachment", positionAttachment);
        json.writeValue("valueAttachments", valueAttachments);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        String effectName = jsonData.getString("effectName");
        String effectPath = parent.getWorkspace().getPath(effectName + ".p");
        FileHandle effectHandle = TalosMain.Instance().ProjectController().findFile(effectPath);
        this.name = effectName;

        //TODO: refactor this
        ParticleEffectDescriptor descriptor = new ParticleEffectDescriptor();
        parent.getWorkspace().getAssetProvider().setParticleFolder(effectHandle.parent().path());
        descriptor.setAssetProvider(parent.getWorkspace().getAssetProvider());
        descriptor.load(effectHandle);
        parent.getWorkspace().getVfxLibrary().put(name, descriptor);

        this.particleEffectDescriptor = descriptor;

        setForever(true); // change this

        positionAttachment = json.readValue(AttachmentPoint.class, jsonData.get("positionAttachment"));
        JsonValue valueAttachmentsJson = jsonData.get("valueAttachments");
        for(JsonValue valueAttachmentJson: valueAttachmentsJson) {
            AttachmentPoint point = json.readValue(AttachmentPoint.class, valueAttachmentJson);
            valueAttachments.add(point);
        }
    }

    public String getStartEvent() {
        return startEvent;
    }

    public String getCompleteEvent() {
        return completeEvent;
    }
}
