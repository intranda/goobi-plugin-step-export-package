package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.export.download.ExportMets;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;

@PluginImplementation
@Log4j2
public class ExportPackageStepPlugin implements IStepPluginVersion2 {
    
    @Getter
    private String title = "intranda_step_generateExportPackage";
    @Getter
    private Step step;
    private String target;
    private String returnPath;
    private List<String> imagefolders = null;
    private boolean includeOcr = false;
    private boolean includeSource = false;
    private boolean includeImport = false;
    private boolean includeExport = false;
    private boolean includeITM = false;
    private boolean includeValidation = false;
    
    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
                
        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        target = myconfig.getString("target", "/opt/digiverso/export/"); 
        imagefolders = Arrays.asList(myconfig.getStringArray("imagefolder"));
        includeOcr = myconfig.getBoolean("ocr", false);
        includeSource = myconfig.getBoolean("source", false);
        includeImport = myconfig.getBoolean("import", false);
        includeExport = myconfig.getBoolean("export", false);
        includeITM = myconfig.getBoolean("itm", false);
        includeValidation = myconfig.getBoolean("validation", false);
        
        log.info("GeneratePackage step plugin initialized");
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return null;
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }
    
    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }
    
    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {
        boolean successful = false;
        
        // first make sure that the destination folder exists
        Path destination = Paths.get(target, step.getProzess().getTitel());
        if (!Files.exists(destination)) {
            try {
                Files.createDirectories(destination);
            } catch (IOException e) {
                log.error("Error during generation of destination path", e);
                Helper.addMessageToProcessLog(step.getProzess().getId(), LogType.ERROR,
                        "Error during generation of destination path: " + e.getMessage());
            }
        }
        
        // do the regular export of the METS file
        ExportMets em = new ExportMets();
        try {
            successful = em.startExport(step.getProzess(), destination.toString() + FileSystems.getDefault().getSeparator());
        } catch (PreferencesException | WriteException | DocStructHasNoTypeException | MetadataTypeNotAllowedException
                | ReadException | TypeNotAllowedForParentException | IOException | InterruptedException
                | ExportFileException | UghHelperException | SwapException | DAOException e) {
            log.error("Error during METS export in package generation", e);
            Helper.addMessageToProcessLog(step.getProzess().getId(), LogType.ERROR,
                    "Error during METS export in package generation: " + e.getMessage());
            Helper.setFehlerMeldung("Error during METS export in package generation", e);
        }
        
        // export images folders as well
        try {
            for (String f : imagefolders) {
                Path folder = Paths.get(step.getProzess().getConfiguredImageFolder(f));
                if (StorageProvider.getInstance().isFileExists(folder)) {
                    StorageProvider.getInstance().copyDirectory(folder, Paths.get(destination.toString(), folder.getFileName().toString()));
                }
            }
        } catch (IOException | InterruptedException | SwapException | DAOException e) {
            successful = false;
            log.error("Error during folder export in package generation", e);
            Helper.addMessageToProcessLog(step.getProzess().getId(), LogType.ERROR,
                    "Error during folder export in package generation: " + e.getMessage());
        }
        
        try {
            // export ocr results
            if (includeOcr) {
                Path ocrFolder = Paths.get(step.getProzess().getOcrDirectory());
                if (ocrFolder != null && Files.exists(ocrFolder)) {
                    List<Path> ocrData = StorageProvider.getInstance().listFiles(ocrFolder.toString());
                    for (Path path : ocrData) {
                        if (Files.isDirectory(path)) {
                            StorageProvider.getInstance().copyDirectory(path, Paths.get(destination.toString(), path.getFileName().toString()));
                        } else {
                            StorageProvider.getInstance().copyFile(path, Paths.get(destination.toString(), path.getFileName().toString()));
                        }
                    }
                }
            }
            
            // export source folder
            if (includeSource) {
                Path sourceFolder = Paths.get(step.getProzess().getSourceDirectory());
                if (sourceFolder != null && Files.exists(sourceFolder)) {
                    StorageProvider.getInstance().copyDirectory(sourceFolder, Paths.get(destination.toString(), step.getProzess().getTitel() + "_source"));
                }
            }
            
            // export import folder
            if (includeImport) {
                Path importFolder = Paths.get(step.getProzess().getImportDirectory());
                if (importFolder != null && Files.exists(importFolder)) {
                    StorageProvider.getInstance().copyDirectory(importFolder, Paths.get(destination.toString(), step.getProzess().getTitel() + "_import"));
                }
            }
            
            // export export folder
            if (includeExport) {
                Path exportFolder = Paths.get(step.getProzess().getExportDirectory());
                if (exportFolder != null && Files.exists(exportFolder)) {
                    StorageProvider.getInstance().copyDirectory(exportFolder, Paths.get(destination.toString(), step.getProzess().getTitel() + "_export"));
                }
            }
            
            // export ITM folder
            if (includeITM) {
                Path itmFolder = Paths.get(step.getProzess().getProcessDataDirectory() + "taskmanager");
                if (itmFolder != null && Files.exists(itmFolder)) {
                    StorageProvider.getInstance().copyDirectory(itmFolder, Paths.get(destination.toString(), itmFolder.getFileName().toString()));
                }
            }
            
            // export validation folder
            if (includeValidation) {
                Path validationFolder = Paths.get(step.getProzess().getProcessDataDirectory() + "validation");
                if (validationFolder != null && Files.exists(validationFolder)) {
                    StorageProvider.getInstance()
                            .copyDirectory(validationFolder, Paths.get(destination.toString(), validationFolder.getFileName().toString()));
                }
            }
        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            successful = false;
            log.error("Error during additional folder export in package generation", e);
            Helper.addMessageToProcessLog(step.getProzess().getId(), LogType.ERROR,
                    "Error during additional folder export in package generation: " + e.getMessage());
        }
        
        log.info("GeneratePackage step plugin executed");
        if (!successful) {
            return PluginReturnValue.ERROR;
        }
        return PluginReturnValue.FINISH;
    }
    
    
}