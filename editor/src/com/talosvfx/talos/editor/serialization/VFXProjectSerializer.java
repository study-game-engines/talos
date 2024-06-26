/*******************************************************************************
 * Copyright 2019 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.talosvfx.talos.editor.serialization;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;
import com.talosvfx.talos.editor.project2.TalosVFXUtils;
import com.talosvfx.talos.editor.wrappers.ModuleWrapper;
import com.talosvfx.talos.editor.wrappers.WrapperRegistry;
import com.talosvfx.talos.runtime.utils.TempHackUtil;
import com.talosvfx.talos.runtime.vfx.ParticleEmitterDescriptor;
import com.talosvfx.talos.runtime.vfx.modules.VectorFieldModule;
import com.talosvfx.talos.runtime.vfx.serialization.ConnectionData;
import com.talosvfx.talos.runtime.vfx.serialization.ExportData;

public class VFXProjectSerializer {

    public VFXProjectSerializer () {
    }

    /**
     * Very naughty
     * @param data
     */

    public static VFXProjectData readTalosTLSProject (FileHandle fileHandle, String talosIdentifier) {
        if(!fileHandle.exists()) return null;
        return readTalosTLSProject(TempHackUtil.hackIt(fileHandle.readString()), talosIdentifier);
    }

    public static VFXProjectData readTalosTLSProject (String data, String talosIdentifier) {
        Json json = new Json();
        json.setIgnoreUnknownFields(true);
        ParticleEmitterDescriptor.registerModules();
        for (Class clazz: WrapperRegistry.map.values()) {
            json.addClassTag(clazz.getSimpleName(), clazz);
        }
        for (Class clazz: ParticleEmitterDescriptor.registeredModules) {
            json.addClassTag(clazz.getSimpleName(), clazz);
        }
        JsonReader reader = new JsonReader();
        JsonValue parse = reader.parse(data);
        parse.addChild("talosIdentifier", new JsonValue(talosIdentifier));
        return json.readValue(VFXProjectData.class, parse);
    }

    public void write (FileHandle fileHandle, VFXProjectData VFXProjectData) {
        fileHandle.writeString(write(VFXProjectData), false);
    }

    public String write (VFXProjectData VFXProjectData) {
        Json json = new Json();
        ParticleEmitterDescriptor.registerModules();
        for (Class clazz: WrapperRegistry.map.values()) {
            json.addClassTag(clazz.getSimpleName(), clazz);
        }
        for (Class clazz: ParticleEmitterDescriptor.registeredModules) {
            json.addClassTag(clazz.getSimpleName(), clazz);
        }
        json.setOutputType(JsonWriter.OutputType.json);
        String data = json.prettyPrint(VFXProjectData);

        return data;
    }

    public static String writeTalosPExport (ExportData exportData) {
        Json json = new Json();
        ParticleEmitterDescriptor.registerModules();
        for (Class clazz: ParticleEmitterDescriptor.registeredModules) {
            json.addClassTag(clazz.getSimpleName(), clazz);
        }

        json.setOutputType(JsonWriter.OutputType.json);
        String data = json.toJson(exportData);

        return data;
    }

    public static ExportData exportTLSDataToP (VFXProjectData projectDataToConvert) {
        Array<EmitterData> emitters = projectDataToConvert.getEmitters();

        ExportData data = new ExportData();

        for (EmitterData emitter : emitters) {
            ExportData.EmitterExportData emitterData = new ExportData.EmitterExportData();
            emitterData.name = emitter.name;
            for (ModuleWrapper wrapper : emitter.modules) {
                emitterData.modules.add(wrapper.getModule());

                if (wrapper.getModule() instanceof VectorFieldModule) {
                    VectorFieldModule vectorFieldModule = (VectorFieldModule) wrapper.getModule();
                    String fgaFileName = vectorFieldModule.fgaFileName;

                    if (fgaFileName == null) {
                        continue;
                    }
                    fgaFileName = fgaFileName + ".fga";
                    if (!data.metadata.resources.contains(fgaFileName, false)) {
                        data.metadata.resources.add(fgaFileName);
                    }
                }
            }

            Array<ConnectionData> connections = emitter.connections;
            for (ConnectionData connection : connections) {
                emitterData.connections.add(connection);
            }

            data.emitters.add(emitterData);
        }

        return data;
    }

}
