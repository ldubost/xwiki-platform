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
package org.xwiki.test.ui.po.editor;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.FormElement;

/**
 * Represents the common actions possible on all Pages when using the "edit" action with the "object" editor.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class ObjectEditPage extends EditPage
{
    @FindBy(id = "update")
    private WebElement objectForm;

    @FindBy(id = "classname")
    private WebElement classNameField;

    @FindBy(name = "action_objectadd")
    private WebElement classNameSubmit;

    private FormElement form;

    /**
     * Go to the passed page in object edit mode.
     */
    public static ObjectEditPage gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page, "edit", "editor=object");
        return new ObjectEditPage();
    }

    public FormElement addObject(String className)
    {
        getForm().setFieldValue(this.classNameField, className);

        final By objectsLocator = By.cssSelector("[id='xclass_" + className + "'] .xobject");
        final int initialObjectCount = getUtil().findElementsWithoutWaiting(getDriver(), objectsLocator).size();
        this.classNameSubmit.click();

        // Make sure we wait for the element to appear since there's no page refresh.
        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return Boolean.valueOf(driver.findElements(objectsLocator).size() > initialObjectCount);
            }
        });

        List<FormElement> objects = getObjectsOfClass(className);
        return objects.get(objects.size() - 1);
    }

    public FormElement addObjectFromInlineLink(String className)
    {
        final By objectsLocator = By.cssSelector("[id='xclass_" + className + "'] .xobject");
        final int initialObjectCount = getDriver().findElements(objectsLocator).size();

        getDriver().findElement(By.cssSelector("[id='add_xobject_" + className + "'] .xobject-add-control")).click();

        // Make sure we wait for the element to appear since there's no page refresh.
        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return Boolean.valueOf(driver.findElements(objectsLocator).size() > initialObjectCount);
            }
        });

        List<FormElement> objects = getObjectsOfClass(className);
        return objects.get(objects.size() - 1);
    }

    public void deleteObject(String className, int index)
    {
        final By objectLocator = By.id("xobject_" + className + "_" + index);
        final WebElement objectContainer = getDriver().findElement(objectLocator);
        WebElement deleteLink = objectContainer.findElement(By.className("delete"));
        deleteLink.click();

        // Expect a confirmation box
        waitUntilElementIsVisible(By.className("xdialog-box-confirmation"));
        getDriver().findElement(By.cssSelector(".xdialog-box-confirmation input[value='Yes']")).click();
        waitUntilElementDisappears(objectLocator);
    }

    public void removeAllDeprecatedProperties()
    {
        getDriver().findElement(By.className("syncAllProperties")).click();
        waitUntilElementDisappears(By.className("deprecatedProperties"));
    }

    /**
     * @param className a class name
     * @param propertyName a class field name
     * @return {@code true} if the specified class field is listed as deprecated, {@code false} otherwise
     */
    public boolean isPropertyDeprecated(String className, String propertyName)
    {
        WebElement xclass = getDriver().findElement(By.id("xclass_" + className));
        List<WebElement> deprecatedPropertiesElements = xclass.findElements(By.className("deprecatedProperties"));
        if (deprecatedPropertiesElements.size() > 0) {
            String xpath = "//label[. = '" + propertyName + ":']";
            return deprecatedPropertiesElements.get(0).findElements(By.xpath(xpath)).size() > 0;
        }
        return false;
    }

    private FormElement getForm()
    {
        if (this.form == null) {
            this.form = new FormElement(this.objectForm);
        }
        return this.form;
    }

    public String getURL(String space, String page)
    {
        return getUtil().getURL(space, page, "edit", "editor=object");
    }

    /** className will look something like "XWiki.XWikiRights" */
    public List<FormElement> getObjectsOfClass(String className)
    {
        List<WebElement> titles =
            getDriver().findElement(By.id("xclass_" + className)).findElements(By.className("xobject-title"));
        List<WebElement> elements =
            getDriver().findElement(By.id("xclass_" + className)).findElements(By.className("xobject-content"));
        List<FormElement> forms = new ArrayList<FormElement>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            WebElement element = elements.get(i);
            // Make sure all forms are displayed otherwise we can't interact with them.
            if (!element.isDisplayed()) {
                titles.get(i).click();
            }
            forms.add(new FormElement(element));
        }
        return forms;
    }
}
