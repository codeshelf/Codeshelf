/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTagView.java,v 1.1 2011/01/21 01:08:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.treeviewers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.dao.IDAOListener;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.ui.LocaleUtils;
import com.gadgetworks.codeshelf.ui.wizards.PickTagDeviceWizard;

// --------------------------------------------------------------------------
/**
 * PickTagView shows a treeviewer of the PickTags and their associate PickTagModules.
 * 
 * @author jeffw
 */
public final class PickTagView implements ISelectionChangedListener, IDoubleClickListener, MenuListener, IDAOListener {

	//	private static final Log			LOGGER				= LogFactory.getLog(PickTagView.class);

	private static final int			ID_COL_WIDTH		= 150;
	private static final int			DESC_COL_WIDTH		= 200;
	private static final int			DETAILS_COL_WIDTH	= 600;

	private static final Log			LOGGER				= LogFactory.getLog(PickTagView.class);

	private Shell						mShell;
	private TreeViewer					mTreeViewer;
	private Tree						mTree;
	private PickTagViewContentProvider	mPickTagViewContentProvider;
	private Menu						mPopup;
	private IController					mController;

	public PickTagView(final Composite inParent, final IController inController, final int inStyle) {

		mController = inController;
		mShell = inParent.getShell();

		mTreeViewer = new TreeViewer(inParent, SWT.SINGLE | SWT.FULL_SELECTION);
		mTree = mTreeViewer.getTree();

		mTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mTree.setFont(Util.getFontRegistry().get(Util.DATOBLOKVIEW_TEXT));
		mTree.setHeaderVisible(true);
		mTree.setLinesVisible(false);
		mTree.setBackground(Util.getColorRegistry().get(Util.BACKGROUND_COLOR));

		//setupToolTips();

		// Create a popup for the items in the person tree.
		mPopup = new Menu(mTree);
		mTree.setMenu(mPopup);
		mPopup.addMenuListener(this);

		// Create a drag source on the tree.
		DragSource dragSource = new DragSource(mTree, DND.DROP_MOVE);
		dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance() });
		dragSource.addDragListener(new DragSourceAdapter() {
			public void dragStart(DragSourceEvent inEvent) {
				PersistABC persistentObject = (PersistABC) mTree.getSelection()[0].getData();
				if (!(persistentObject instanceof PickTagModule)) {
					inEvent.doit = false;
				}
				LOGGER.info(persistentObject);
			}

			public void dragSetData(DragSourceEvent inEvent) {
				// Set the data to be the first selected item's text
				PersistABC persistentObject = (PersistABC) mTree.getSelection()[0].getData();
				inEvent.data = persistentObject.getPersistentId().toString();
			}
		});

		// Create the drop target on the tree.
		Transfer[] transfers = new Transfer[] { TextTransfer.getInstance() };
		mTreeViewer.addDropSupport(DND.DROP_MOVE, transfers, new PickTagViewDropAdapter(mTreeViewer));

		TreeColumn column = new TreeColumn(mTree, SWT.NONE);
		column.setWidth(ID_COL_WIDTH);
		column.setText(LocaleUtils.getStr("datoblokview.id_col.label"));
		column.setResizable(true);
		column.setData(PickTagViewDecoratedLabelProvider.ID_COL);

		column = new TreeColumn(mTree, SWT.NONE);
		column.setWidth(DESC_COL_WIDTH);
		column.setText(LocaleUtils.getStr("datoblokview.desc_col.label"));
		column.setResizable(true);
		column.setMoveable(true);
		column.setData(PickTagViewDecoratedLabelProvider.DESC_COL);

		column = new TreeColumn(mTree, SWT.NONE);
		column.setWidth(DETAILS_COL_WIDTH);
		column.setText(LocaleUtils.getStr("datoblokview.details_col.label"));
		column.setResizable(true);
		column.setMoveable(true);
		column.setData(PickTagViewDecoratedLabelProvider.DETAILS_COL);

		mPickTagViewContentProvider = new PickTagViewContentProvider();

		mTreeViewer.setContentProvider(mPickTagViewContentProvider);
		mTreeViewer.setComparator(new PickTagViewSorter());
		mTreeViewer.setLabelProvider(new PickTagViewDecoratedLabelProvider(mTree,
			new PickTagViewLabelProvider(),
			new PickTagViewDecorator()));
		//		mTreeViewer.addFilter(new ActiveAccountsFilter());
		//		mTreeViewer.setComparer(new ObjectIDComparer());
		mTreeViewer.addSelectionChangedListener(this);
		mTreeViewer.addDoubleClickListener(this);
		mTreeViewer.setInput(PickTagViewContentProvider.DATOBLOK_ROOT);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public IStructuredSelection getTreeSelection() {
		return (IStructuredSelection) (mTreeViewer.getSelection());
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inListener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener inListener) {
		mTreeViewer.addSelectionChangedListener(inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent inEvent) {
		IStructuredSelection selection = (IStructuredSelection) inEvent.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof PickTag) {
			//mParentAppWindow.switchToOwnerAccount((Account) element);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	public void doubleClick(DoubleClickEvent inEvent) {
		IStructuredSelection selection = (IStructuredSelection) inEvent.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof PickTag) {
			//mParentAppWindow.switchToOwnerAccount((Account) element);
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.MenuListener#menuHidden(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuHidden(MenuEvent inEvent) {

	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.MenuListener#menuShown(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuShown(MenuEvent inMenuEvent) {

		// Get rid of existing menu items
		MenuItem[] items = mPopup.getItems();
		for (int i = 0; i < items.length; i++) {
			((MenuItem) items[i]).dispose();
		}

		// Figure out what, if anything the user selected.
		Object itemData = null;
		if (mTree.getSelectionCount() > 0) {
			// Add menu items for current selection
			TreeItem treeItem = mTree.getSelection()[0];
			if (treeItem != null) {
				itemData = ((TreeItem) treeItem).getData();
			}
		}

		if (itemData == null) {
			datoblokMenu(null);
		} else if (itemData instanceof PickTag) {
			PickTag datoblok = (PickTag) itemData;
			datoblokMenu(datoblok);
		} else if (itemData instanceof PickTagModule) {
			PickTagModule datoBlokModule = (PickTagModule) itemData;
			datoBlokModuleMenu(datoBlokModule);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * This is the popup menu when the user clicks on an PickTag in the PickTagView.
	 * 
	 * @param inPickTag
	 */
	private void datoblokMenu(final PickTag inPickTag) {

		final MenuItem addItem = new MenuItem(mPopup, SWT.NONE);
		addItem.setText(LocaleUtils.getStr("datoblokview.menu.add_datoblok"));
		addItem.setData(null);
		addItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				PickTagDeviceWizard.createPickTagDevice(Display.getCurrent().getActiveShell(), mController);
			}
		});

		final MenuItem editItem = new MenuItem(mPopup, SWT.NONE);
		editItem.setText(LocaleUtils.getStr("datoblokview.menu.edit_datoblok"));
		editItem.setData(inPickTag);
		editItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				PickTag datoblok = Util.getSystemDAO().loadPickTag(inPickTag.getPersistentId());
				PickTagDeviceWizard.editPickTagDevice(datoblok, mController, mShell);
			}
		});

		final MenuItem deleteItem = new MenuItem(mPopup, SWT.NONE);
		deleteItem.setText(LocaleUtils.getStr("datoblokview.menu.delete_datoblok"));
		deleteItem.setData(inPickTag);
		deleteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				boolean deletePickTag = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
					LocaleUtils.getStr("datoblokview.menu.delete_datoblok.title"),
					LocaleUtils.getStr("datoblokview.menu.delete_datoblok.prompt", new String[] { inPickTag.getDescription() }));
				if (deletePickTag) {
					try {
						if (inPickTag instanceof PickTag) {
							Util.getSystemDAO().deletePickTag((PickTag) inPickTag);
						}
					} catch (DAOException e) {
						LOGGER.error("", e);
					}
				}
			}
		});

		new MenuItem(mPopup, SWT.SEPARATOR);

		// Add module menu item.
		final MenuItem adItem = new MenuItem(mPopup, SWT.NONE);
		adItem.setText(LocaleUtils.getStr("datoblokview.menu.add_datoblokmodule"));
		adItem.setData(inPickTag);
		adItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
//				RulesetWizard.addPickTagRuleset(inPickTag);
			}
		});

		// Decide which menu items should be enabled.
		if (inPickTag == null) {
			addItem.setEnabled(true);
			editItem.setEnabled(false);
			deleteItem.setEnabled(false);
		} else {
			addItem.setEnabled(true);
			editItem.setEnabled(true);
			deleteItem.setEnabled(true);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * This is the popup menu when the user clicks on a PickTagModule in the PickTagView.
	 * 
	 * @param inPickTagModule
	 */
	private void datoBlokModuleMenu(final PickTagModule inPickTagModule) {

		// Add PickTagModule menu item.
		final MenuItem adItem = new MenuItem(mPopup, SWT.NONE);
		adItem.setText(LocaleUtils.getStr("datoblokview.menu.add_datoblokmodule"));
		adItem.setData(inPickTagModule.getParentPickTag());
		adItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
//				RulesetWizard.addPickTagRuleset(inPickTagModule.getParentPickTag());
			}
		});

		// Edit PickTagModule menu item.
		final MenuItem editItem = new MenuItem(mPopup, SWT.NONE);
		editItem.setText(LocaleUtils.getStr("datoblokview.menu.edit_datoblockmodule"));
		editItem.setData(inPickTagModule);
		editItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
//				RulesetWizard.editPickTagRuleset(inPickTagModule);
			}
		});

		// Delete PickTagModule menu item.
		final MenuItem deleteItem = new MenuItem(mPopup, SWT.NONE);
		deleteItem.setText(LocaleUtils.getStr("datoblokview.menu.delete_datoblokmodule"));
		deleteItem.setData(inPickTagModule);
		deleteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				boolean deletePickTagModule = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
					LocaleUtils.getStr("datoblokview.menu.delete_datoblokmodule.title"),
					LocaleUtils.getStr("datoblokview.menu.delete_datoblokmodule.prompt",
						new String[] { inPickTagModule.getDescription() }));
				if (deletePickTagModule) {
					try {
						Util.getSystemDAO().deletePickTagModule(inPickTagModule);
					} catch (DAOException e) {
						LOGGER.error(e);
					}
				}
			}
		});

		new MenuItem(mPopup, SWT.SEPARATOR);

		editItem.setEnabled(true);
		deleteItem.setEnabled(true);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void switchToSelection() {
		if (mTreeViewer.getTree().getSelectionCount() > 0) {
			Object object = mTreeViewer.getTree().getSelection()[0].getData();
			if (object instanceof PickTag) {
				//mParentAppWindow.switchToOwnerAccount((Account) object);
			}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectAdded(java.lang.Object)
	 */
	public void objectAdded(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			if (inObject instanceof PickTag) {
				mTreeViewer.add(PickTagViewContentProvider.DATOBLOK_ROOT, inObject);
			} else if (inObject instanceof PickTagModule) {
				PickTagModule datoBlokModule = (PickTagModule) inObject;
				PickTag datoblok = datoBlokModule.getParentPickTag();
				if (datoblok != null) {
					mTreeViewer.add(datoblok, inObject);
				}
			}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectDeleted(java.lang.Object)
	 */
	public void objectDeleted(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			mTreeViewer.setSelection(null);
			mTreeViewer.remove(inObject);
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectUpdated(java.lang.Object)
	 */
	public void objectUpdated(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			Object refreshParentHoobee = inObject;

			if (inObject instanceof PickTag) {
				refreshParentHoobee = inObject;
			} else if (inObject instanceof PickTagModule) {
				refreshParentHoobee = ((PickTagModule) inObject).getParentPickTag();
			}

			String[] properties = { " " };
			mTreeViewer.update(inObject, properties);
			if (refreshParentHoobee != null) {
				mTreeViewer.refresh(refreshParentHoobee, true);
			}
			expandOnlinePickTagz();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inObject
	 *  @return
	 */
	private boolean doesDAOChangeApply(Object inObject) {
		boolean result = false;

		// We only want to update if it's an account or buddy update.
		if (inObject instanceof PickTag) {
			result = true;
		} else if (inObject instanceof PickTagModule) {
			result = true;
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void expandOnlinePickTagz() {

		// Expand any accounts that are online.
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				for (TreeItem treeItem : mTree.getItems()) {
					if (treeItem.getData() instanceof PickTag) {
						if (!treeItem.getExpanded()) {
							PickTag datoblok = (PickTag) treeItem.getData();
							if (datoblok.getNetworkDeviceState().equals(NetworkDeviceStateEnum.STARTED)) {
								mTreeViewer.expandToLevel(treeItem.getData(), 1);
							}
						}
					}
				}
			}
		});

	}

	private static class PickTagViewDropAdapter extends ViewerDropAdapter {

		public PickTagViewDropAdapter(final TreeViewer inTreeViewer) {
			super(inTreeViewer);
		}

		/* --------------------------------------------------------------------------
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
		 */
		public boolean performDrop(Object inData) {
			PickTagModule datoBlokModule = Util.getSystemDAO().loadPickTagModule(Integer.valueOf((String) inData));
			if (datoBlokModule != null) {
				PickTag parentPickTag = datoBlokModule.getParentPickTag();
				if (getCurrentTarget() instanceof PickTag) {
					PickTag datoblok = (PickTag) getCurrentTarget();
					datoblok.addPickTagModule(datoBlokModule);
					datoBlokModule.setParentPickTag(datoblok);
					try {
						Util.getSystemDAO().storePickTagModule(datoBlokModule);
						Util.getSystemDAO().storePickTag(datoblok);
					} catch (DAOException e) {
						LOGGER.error("", e);
					}
					Util.getSystemDAO().pushNonPersistentUpdates(parentPickTag);
				}
			}
			return true;
		}

		/* --------------------------------------------------------------------------
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
		 */
		public boolean validateDrop(Object inTarget, int inOperation, TransferData inTransferType) {
			boolean result = false;
			if (inTarget instanceof PickTag) {
				result = true;
			}
			return result;
		}

	}
}
