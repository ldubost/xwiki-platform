/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.officeimporter.internal.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net.sf.jodconverter.DefaultDocumentFormatRegistry;
import net.sf.jodconverter.DocumentFormat;
import net.sf.jodconverter.DocumentFormatRegistry;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.internal.OfficeImporterFileStorage;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerManager;
import org.xwiki.officeimporter.transformer.DocumentTransformer;

/**
 * Transforms an Office Document into a corresponding Html document.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class OfficeToHtmlTransformer extends AbstractLogEnabled implements DocumentTransformer, Initializable
{
    /**
     * Default encoding for office imported documents.
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    /**
     * The {@link OpenOfficeServerManager} component.
     */
    private OpenOfficeServerManager ooManager;

    /**
     * Output format of this transformer.
     */
    private DocumentFormat htmlFormat;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        DocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
        htmlFormat = formatRegistry.getFormatByExtension("html");
    }

    /**
     * {@inheritDoc}
     */
    public void transform(OfficeImporterContext importerContext) throws OfficeImporterException
    {
        // Make sure the openoffice server is running.    
        if (ooManager.getServerState() != OpenOfficeServerManager.ServerState.RUNNING) {
            throw new OfficeImporterException("OpenOffice server is unavailable.");
        }
        // Prepare the temporary directory structure.
        OfficeImporterFileStorage storage =
            new OfficeImporterFileStorage("xwiki-office-importer-" + importerContext.getCurrentUser());
        // Fill in the input file.
        try {
            FileOutputStream fos = new FileOutputStream(storage.getInputFile());
            fos.write(importerContext.getSourceData());
            fos.close();
        } catch (IOException ex) {
            String message = "Internal error while creating temporary files.";
            getLogger().error(message, ex);
            storage.cleanUp();
            throw new OfficeImporterException(message, ex);
        }
        // Make the conversion.
        ooManager.getDocumentConverter().convert(storage.getInputFile(), storage.getOutputFile(), htmlFormat);
        // Collect the output into context. First the html output.
        try {
            FileInputStream fis = new FileInputStream(storage.getOutputFile());
            byte[] content = new byte[(int) storage.getOutputFile().length()];
            fis.read(content);
            fis.close();
            importerContext.setContent(getEncodedHtml(new String(content)));
        } catch (IOException ex) {
            String message = "Internal error while reading temporary files.";
            getLogger().error(message, ex);
            storage.cleanUp();
            throw new OfficeImporterException(message, ex);
        }
        // Start collecting the artifacts.
        File[] artifacts = storage.getOutputDir().listFiles();
        for (File artifact : artifacts) {
            try {
                FileInputStream fis = new FileInputStream(artifact);
                byte[] data = new byte[(int) artifact.length()];
                fis.read(data);
                importerContext.addArtifact(artifact.getName(), data);
            } catch (IOException ex) {
                String message = "Internal error while reading artifact : " + artifact.getName();
                getLogger().error(message, ex);
                // Skip the artifact.
            }
        }
        // Cleanup the mess.
        storage.cleanUp();
    }
    
    /**
     * Encodes the given html fragment with the default encoding for oo.
     * 
     * @param html the html content.
     * @return the html string encoded with default encoding for oo server.
     */
    public String getEncodedHtml(String html) throws OfficeImporterException
    {
        try {
            return new String(html.getBytes(), DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            throw new OfficeImporterException("Inernal error while encoding document content.", ex);
        }
    }
}
