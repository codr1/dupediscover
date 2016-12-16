package com.codr1.dupediscover;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.data.Item;
import com.vaadin.ui.UI;
import com.vaadin.ui.Table;
import com.vaadin.data.util.FilesystemContainer;
import java.io.File;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.TextField;
import java.io.FilenameFilter;

/**
 *
 */
@Theme("mytheme")
@Widgetset("com.mycompany.dupediscover.MyAppWidgetset")
public class DupeDiscoverUI extends UI {

    FilenameFilter fileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            File file = new File( dir.getAbsolutePath() + "/" + name );
            return file.isDirectory();
            
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
            
    FilesystemContainer files = new FilesystemContainer( new File( "/" ), fileFilter, false );
    TreeTable availableDirectories = new TreeTable( "Directory Tree", files );
    Table selectedDirectories = new Table( "Selected Directories" );
    final Button startScan = new Button( "Start Scan" );
    final Button cancelScan = new Button( "Cancel Scan" );
    TextField progress = new TextField();
    
    
    @Override
    protected void init(VaadinRequest vaadinRequest) {
        VerticalSplitPanel vSplitLeft = new VerticalSplitPanel();
        VerticalSplitPanel vSplitRight = new VerticalSplitPanel();
        VerticalLayout topRightVertical = new VerticalLayout();
        VerticalLayout topLeftVertical = new VerticalLayout();
        HorizontalLayout topRightButtonRibbon = new HorizontalLayout();
        
        HorizontalSplitPanel hSplit = new HorizontalSplitPanel(vSplitLeft, vSplitRight);
        
        selectedDirectories.addContainerProperty( "Name", String.class, null );
        
        vSplitLeft.addComponent( topLeftVertical );
        vSplitRight.addComponent( topRightVertical );
        topLeftVertical.addComponent(availableDirectories);
        topRightVertical.addComponent(selectedDirectories);
        topRightVertical.addComponent(topRightButtonRibbon);
        topRightButtonRibbon.addComponent( startScan );
        topRightButtonRibbon.addComponent( cancelScan );
        topRightButtonRibbon.addComponent(progress);
        
        
        
        availableDirectories.setVisibleColumns("Name", "Last Modified");
        availableDirectories.setColumnExpandRatio( "Name", 1 );
        availableDirectories.setColumnExpandRatio( "Last Modified", 0 );
        availableDirectories.setWidth("100%");
        availableDirectories.setHeight("100%");
        
        selectedDirectories.setWidth("100%");
        selectedDirectories.setHeight("100%");
        
        cancelScan.setEnabled(false);
        
        availableDirectories.addGeneratedColumn("add", new TreeTable.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, final Object itemId, Object columnId) {
                Button buttonAdd = new Button("Check for Dupes");
                buttonAdd.addClickListener( new Button.ClickListener() {
                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        
                        Item queryRow = selectedDirectories.getItem(itemId);
                        if( queryRow != null ) {
                            Notification.show(" Directory already selected ");
                            return;
                        } 
                        
                        Item newRow = selectedDirectories.addItem( itemId );
                        newRow.getItemProperty("Name").setValue( "boo" /*itemId.toString()*/ );  
                    }
                });
                return buttonAdd;
            }
        });
        
        selectedDirectories.addGeneratedColumn("remove", new TreeTable.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, final Object itemId, Object columnId) {
                Button buttonRemove = new Button( "Remove" );
                buttonRemove.addClickListener( new Button.ClickListener() {
                    @Override
                    public void buttonClick( Button.ClickEvent event ) {
                        Object targetItemId = selectedDirectories.removeItem(itemId);
                    }
                });
                return buttonRemove;
            }
        });
        
        startScan.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                cancelScan.setEnabled(true);
                startScan.setEnabled(false);
            }
        });
        
        
        cancelScan.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                cancelScan.setEnabled(false);
                startScan.setEnabled(true);
            }
        });
        setContent( hSplit );
        
        //selectedDirectories.setSelectable(true);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = DupeDiscoverUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
