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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
@Theme("mytheme")
@Widgetset("com.codr1.dupediscover.MyAppWidgetset")
public class DupeDiscoverUI extends UI {

    FilenameFilter fileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            File file = new File( dir.getAbsolutePath() + "/" + name );
            return file.isDirectory();
            
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    
    public static class Finder 
            extends SimpleFileVisitor<Path> {
        
        void processFile( Path file ) {
            
        }
        
        // Process each file .
        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) {
            processFile (file);
            return CONTINUE;
        }
        
        // Handle error
        @Override
        public FileVisitResult visitFileFailed(Path file,
                IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
        
    }
            
    FilesystemContainer files = new FilesystemContainer( new File( "/" ), fileFilter, false );
    TreeTable availableDirectories = new TreeTable( "Directory Tree", files );
    Table selectedDirectories = new Table( "Selected Directories" );
    final Button startScan = new Button( "Start Scan" );
    final Button cancelScan = new Button( "Cancel Scan" );
    TextField progress = new TextField();
    
    Thread scanThread = new Thread( new Runnable() {
        @Override
        public void run() {
            // Check if there is anything to scan
            if( selectedDirectories.size() < 1 ) {
                Notification.show( "No directories have been selected to be scanned" );
                return;
            }
            
            
            
            //Iterate through the slected directories
            for( Iterator i = selectedDirectories.getItemIds().iterator(); i.hasNext();){
                
            }
            
            
            
            
            
            
           
        }
    });
    
    
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
        topRightButtonRibbon.addComponent( progress );
        
        
        
        availableDirectories.setVisibleColumns("Name", "Last Modified");
        availableDirectories.setColumnExpandRatio( "Name", 1 );
        availableDirectories.setColumnExpandRatio( "Last Modified", 0 );
        availableDirectories.setWidth("100%");
        availableDirectories.setHeight("100%");
        
        selectedDirectories.setWidth("100%");
        selectedDirectories.setHeight("100%");
        
        setContent( hSplit );
        
        final Notification notify = new Notification("hi");
        notify.setDelayMsec(20000);
        
        
        //cancelScan.setEnabled(false);
        
        availableDirectories.addGeneratedColumn("add", new TreeTable.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, final Object itemId, Object columnId) {
                Button buttonAdd = new Button("Check for Dupes");
                buttonAdd.addClickListener( new Button.ClickListener() {
                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        
                        // In case we have to delete a row...  you can't delete while iterating,
                        // so we store the item ids in this list and delete once we are all done.
                        List<Object> toDelete = new ArrayList<Object>();
                        //This string will store the names of all deleted directories
                        String deletedDirectories = new String();
                        
                        // Check if we already selected a lower level or a higher level directory
                        for( Iterator i = selectedDirectories.getItemIds().iterator(); i.hasNext();){
                            int id = (Integer) i.next();
                            Item currentRow = selectedDirectories.getItem(id);
                            
                            String currentDirectory = currentRow.getItemProperty("Name").getValue().toString();
                            
                            // check if we have exactly the same name
                            if( itemId.toString().equals( currentDirectory ) ){
                                notify.show(" Directory already selected ");
                                return;
                            }
                            
                            // check if the new directory is a parent directory of the existing entry
                            if( currentDirectory.contains(itemId.toString())){
                                // we can't remove the item yet while we are iterating.  we will store it and remove it later
                                toDelete.add((Object)id);
                                deletedDirectories = deletedDirectories.concat( "\n" + currentDirectory + "\n");
                            }
                            
                            // Check if the new directory is a sub directory of the existing directory
                            if( itemId.toString().contains( currentDirectory )) {
                                notify.show( "Not adding " + itemId.toString() + " since its parent is already selected." );
                                return;
                            }
                        }
                        
                        //Check if we have any items slated for deletion and take them out
                        for (Object id : toDelete) {        
                            selectedDirectories.removeItem(id);
                        }
                        if( deletedDirectories.isEmpty() == false ){
                            notify.show( "Since we added their parent directory, the following have been removed:\n"+ deletedDirectories );
                            
                        }
                        
                        Object newRowId = selectedDirectories.addItem( );
                        Item newRow = selectedDirectories.getItem( newRowId );
                        newRow.getItemProperty("Name").setValue( itemId.toString() );  
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
                        selectedDirectories.removeItem(itemId);
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
        
        
        
        //selectedDirectories.setSelectable(true);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = DupeDiscoverUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
