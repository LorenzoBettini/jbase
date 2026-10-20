/**
 * Copyright (c) 2010, 2021 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.xtext.example.domainmodel.ui.tests;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.xbase.lib.Pair;
import org.junit.Test;

public class AssociationHierarchyTest {
	@Test
	public void providesLineAndPropertyColumns() throws Exception {
		final var descriptor = PlatformUI.getWorkbench().getViewRegistry().find(
				"org.eclipse.xtext.example.domainmodel.ui.editor.AssociationHierarchy");
		final var view = descriptor.createView();
		final var method = view.getClass().getDeclaredMethod("getLocationColumnDescriptionList");
		method.setAccessible(true);
		final var columns = (List<?>) method.invoke(view);
		assertEquals(2, columns.size());
		assertEquals("Line", ((Pair<?, ?>) columns.getFirst()).getKey());
		assertEquals(60, ((ColumnWeightData) ((Pair<?, ?>) columns.getFirst()).getValue()).weight);
		assertEquals("Property", ((Pair<?, ?>) columns.getLast()).getKey());
		assertEquals(300, ((ColumnWeightData) ((Pair<?, ?>) columns.getLast()).getValue()).weight);
	}

	@Test
	public void registersAssociationHierarchyView() {
		final var descriptor = PlatformUI.getWorkbench().getViewRegistry().find(
				"org.eclipse.xtext.example.domainmodel.ui.editor.AssociationHierarchy");
		assertNotNull(descriptor);
		assertEquals("Association Hierarchy", descriptor.getLabel());
	}

}
