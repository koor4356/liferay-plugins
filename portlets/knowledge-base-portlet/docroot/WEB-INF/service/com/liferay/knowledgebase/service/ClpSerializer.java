/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.knowledgebase.service;

import aQute.bnd.annotation.ProviderType;

import com.liferay.knowledgebase.model.KBArticleClp;
import com.liferay.knowledgebase.model.KBCommentClp;
import com.liferay.knowledgebase.model.KBFolderClp;
import com.liferay.knowledgebase.model.KBTemplateClp;

import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayOutputStream;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.util.ClassLoaderObjectInputStream;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Brian Wing Shun Chan
 */
@ProviderType
public class ClpSerializer {
	public static String getServletContextName() {
		if (Validator.isNotNull(_servletContextName)) {
			return _servletContextName;
		}

		synchronized (ClpSerializer.class) {
			if (Validator.isNotNull(_servletContextName)) {
				return _servletContextName;
			}

			try {
				ClassLoader classLoader = ClpSerializer.class.getClassLoader();

				Class<?> portletPropsClass = classLoader.loadClass(
						"com.liferay.util.portlet.PortletProps");

				Method getMethod = portletPropsClass.getMethod("get",
						new Class<?>[] { String.class });

				String portletPropsServletContextName = (String)getMethod.invoke(null,
						"knowledge-base-portlet-deployment-context");

				if (Validator.isNotNull(portletPropsServletContextName)) {
					_servletContextName = portletPropsServletContextName;
				}
			}
			catch (Throwable t) {
				if (_log.isInfoEnabled()) {
					_log.info(
						"Unable to locate deployment context from portlet properties");
				}
			}

			if (Validator.isNull(_servletContextName)) {
				try {
					String propsUtilServletContextName = PropsUtil.get(
							"knowledge-base-portlet-deployment-context");

					if (Validator.isNotNull(propsUtilServletContextName)) {
						_servletContextName = propsUtilServletContextName;
					}
				}
				catch (Throwable t) {
					if (_log.isInfoEnabled()) {
						_log.info(
							"Unable to locate deployment context from portal properties");
					}
				}
			}

			if (Validator.isNull(_servletContextName)) {
				_servletContextName = "knowledge-base-portlet";
			}

			return _servletContextName;
		}
	}

	public static Object translateInput(BaseModel<?> oldModel) {
		Class<?> oldModelClass = oldModel.getClass();

		String oldModelClassName = oldModelClass.getName();

		if (oldModelClassName.equals(KBArticleClp.class.getName())) {
			return translateInputKBArticle(oldModel);
		}

		if (oldModelClassName.equals(KBCommentClp.class.getName())) {
			return translateInputKBComment(oldModel);
		}

		if (oldModelClassName.equals(KBFolderClp.class.getName())) {
			return translateInputKBFolder(oldModel);
		}

		if (oldModelClassName.equals(KBTemplateClp.class.getName())) {
			return translateInputKBTemplate(oldModel);
		}

		return oldModel;
	}

	public static Object translateInput(List<Object> oldList) {
		List<Object> newList = new ArrayList<Object>(oldList.size());

		for (int i = 0; i < oldList.size(); i++) {
			Object curObj = oldList.get(i);

			newList.add(translateInput(curObj));
		}

		return newList;
	}

	public static Object translateInputKBArticle(BaseModel<?> oldModel) {
		KBArticleClp oldClpModel = (KBArticleClp)oldModel;

		BaseModel<?> newModel = oldClpModel.getKBArticleRemoteModel();

		newModel.setModelAttributes(oldClpModel.getModelAttributes());

		return newModel;
	}

	public static Object translateInputKBComment(BaseModel<?> oldModel) {
		KBCommentClp oldClpModel = (KBCommentClp)oldModel;

		BaseModel<?> newModel = oldClpModel.getKBCommentRemoteModel();

		newModel.setModelAttributes(oldClpModel.getModelAttributes());

		return newModel;
	}

	public static Object translateInputKBFolder(BaseModel<?> oldModel) {
		KBFolderClp oldClpModel = (KBFolderClp)oldModel;

		BaseModel<?> newModel = oldClpModel.getKBFolderRemoteModel();

		newModel.setModelAttributes(oldClpModel.getModelAttributes());

		return newModel;
	}

	public static Object translateInputKBTemplate(BaseModel<?> oldModel) {
		KBTemplateClp oldClpModel = (KBTemplateClp)oldModel;

		BaseModel<?> newModel = oldClpModel.getKBTemplateRemoteModel();

		newModel.setModelAttributes(oldClpModel.getModelAttributes());

		return newModel;
	}

	public static Object translateInput(Object obj) {
		if (obj instanceof BaseModel<?>) {
			return translateInput((BaseModel<?>)obj);
		}
		else if (obj instanceof List<?>) {
			return translateInput((List<Object>)obj);
		}
		else {
			return obj;
		}
	}

	public static Object translateOutput(BaseModel<?> oldModel) {
		Class<?> oldModelClass = oldModel.getClass();

		String oldModelClassName = oldModelClass.getName();

		if (oldModelClassName.equals(
					"com.liferay.knowledgebase.model.impl.KBArticleImpl")) {
			return translateOutputKBArticle(oldModel);
		}
		else if (oldModelClassName.endsWith("Clp")) {
			try {
				ClassLoader classLoader = ClpSerializer.class.getClassLoader();

				Method getClpSerializerClassMethod = oldModelClass.getMethod(
						"getClpSerializerClass");

				Class<?> oldClpSerializerClass = (Class<?>)getClpSerializerClassMethod.invoke(oldModel);

				Class<?> newClpSerializerClass = classLoader.loadClass(oldClpSerializerClass.getName());

				Method translateOutputMethod = newClpSerializerClass.getMethod("translateOutput",
						BaseModel.class);

				Class<?> oldModelModelClass = oldModel.getModelClass();

				Method getRemoteModelMethod = oldModelClass.getMethod("get" +
						oldModelModelClass.getSimpleName() + "RemoteModel");

				Object oldRemoteModel = getRemoteModelMethod.invoke(oldModel);

				BaseModel<?> newModel = (BaseModel<?>)translateOutputMethod.invoke(null,
						oldRemoteModel);

				return newModel;
			}
			catch (Throwable t) {
				if (_log.isInfoEnabled()) {
					_log.info("Unable to translate " + oldModelClassName, t);
				}
			}
		}

		if (oldModelClassName.equals(
					"com.liferay.knowledgebase.model.impl.KBCommentImpl")) {
			return translateOutputKBComment(oldModel);
		}
		else if (oldModelClassName.endsWith("Clp")) {
			try {
				ClassLoader classLoader = ClpSerializer.class.getClassLoader();

				Method getClpSerializerClassMethod = oldModelClass.getMethod(
						"getClpSerializerClass");

				Class<?> oldClpSerializerClass = (Class<?>)getClpSerializerClassMethod.invoke(oldModel);

				Class<?> newClpSerializerClass = classLoader.loadClass(oldClpSerializerClass.getName());

				Method translateOutputMethod = newClpSerializerClass.getMethod("translateOutput",
						BaseModel.class);

				Class<?> oldModelModelClass = oldModel.getModelClass();

				Method getRemoteModelMethod = oldModelClass.getMethod("get" +
						oldModelModelClass.getSimpleName() + "RemoteModel");

				Object oldRemoteModel = getRemoteModelMethod.invoke(oldModel);

				BaseModel<?> newModel = (BaseModel<?>)translateOutputMethod.invoke(null,
						oldRemoteModel);

				return newModel;
			}
			catch (Throwable t) {
				if (_log.isInfoEnabled()) {
					_log.info("Unable to translate " + oldModelClassName, t);
				}
			}
		}

		if (oldModelClassName.equals(
					"com.liferay.knowledgebase.model.impl.KBFolderImpl")) {
			return translateOutputKBFolder(oldModel);
		}
		else if (oldModelClassName.endsWith("Clp")) {
			try {
				ClassLoader classLoader = ClpSerializer.class.getClassLoader();

				Method getClpSerializerClassMethod = oldModelClass.getMethod(
						"getClpSerializerClass");

				Class<?> oldClpSerializerClass = (Class<?>)getClpSerializerClassMethod.invoke(oldModel);

				Class<?> newClpSerializerClass = classLoader.loadClass(oldClpSerializerClass.getName());

				Method translateOutputMethod = newClpSerializerClass.getMethod("translateOutput",
						BaseModel.class);

				Class<?> oldModelModelClass = oldModel.getModelClass();

				Method getRemoteModelMethod = oldModelClass.getMethod("get" +
						oldModelModelClass.getSimpleName() + "RemoteModel");

				Object oldRemoteModel = getRemoteModelMethod.invoke(oldModel);

				BaseModel<?> newModel = (BaseModel<?>)translateOutputMethod.invoke(null,
						oldRemoteModel);

				return newModel;
			}
			catch (Throwable t) {
				if (_log.isInfoEnabled()) {
					_log.info("Unable to translate " + oldModelClassName, t);
				}
			}
		}

		if (oldModelClassName.equals(
					"com.liferay.knowledgebase.model.impl.KBTemplateImpl")) {
			return translateOutputKBTemplate(oldModel);
		}
		else if (oldModelClassName.endsWith("Clp")) {
			try {
				ClassLoader classLoader = ClpSerializer.class.getClassLoader();

				Method getClpSerializerClassMethod = oldModelClass.getMethod(
						"getClpSerializerClass");

				Class<?> oldClpSerializerClass = (Class<?>)getClpSerializerClassMethod.invoke(oldModel);

				Class<?> newClpSerializerClass = classLoader.loadClass(oldClpSerializerClass.getName());

				Method translateOutputMethod = newClpSerializerClass.getMethod("translateOutput",
						BaseModel.class);

				Class<?> oldModelModelClass = oldModel.getModelClass();

				Method getRemoteModelMethod = oldModelClass.getMethod("get" +
						oldModelModelClass.getSimpleName() + "RemoteModel");

				Object oldRemoteModel = getRemoteModelMethod.invoke(oldModel);

				BaseModel<?> newModel = (BaseModel<?>)translateOutputMethod.invoke(null,
						oldRemoteModel);

				return newModel;
			}
			catch (Throwable t) {
				if (_log.isInfoEnabled()) {
					_log.info("Unable to translate " + oldModelClassName, t);
				}
			}
		}

		return oldModel;
	}

	public static Object translateOutput(List<Object> oldList) {
		List<Object> newList = new ArrayList<Object>(oldList.size());

		for (int i = 0; i < oldList.size(); i++) {
			Object curObj = oldList.get(i);

			newList.add(translateOutput(curObj));
		}

		return newList;
	}

	public static Object translateOutput(Object obj) {
		if (obj instanceof BaseModel<?>) {
			return translateOutput((BaseModel<?>)obj);
		}
		else if (obj instanceof List<?>) {
			return translateOutput((List<Object>)obj);
		}
		else {
			return obj;
		}
	}

	public static Throwable translateThrowable(Throwable throwable) {
		if (_useReflectionToTranslateThrowable) {
			try {
				UnsyncByteArrayOutputStream unsyncByteArrayOutputStream = new UnsyncByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(unsyncByteArrayOutputStream);

				objectOutputStream.writeObject(throwable);

				objectOutputStream.flush();
				objectOutputStream.close();

				UnsyncByteArrayInputStream unsyncByteArrayInputStream = new UnsyncByteArrayInputStream(unsyncByteArrayOutputStream.unsafeGetByteArray(),
						0, unsyncByteArrayOutputStream.size());

				Thread currentThread = Thread.currentThread();

				ClassLoader contextClassLoader = currentThread.getContextClassLoader();

				ObjectInputStream objectInputStream = new ClassLoaderObjectInputStream(unsyncByteArrayInputStream,
						contextClassLoader);

				throwable = (Throwable)objectInputStream.readObject();

				objectInputStream.close();

				return throwable;
			}
			catch (ClassNotFoundException cnfe) {
				if (_log.isInfoEnabled()) {
					_log.info("Do not use reflection to translate throwable");
				}

				_useReflectionToTranslateThrowable = false;
			}
			catch (SecurityException se) {
				if (_log.isInfoEnabled()) {
					_log.info("Do not use reflection to translate throwable");
				}

				_useReflectionToTranslateThrowable = false;
			}
			catch (Throwable throwable2) {
				_log.error(throwable2, throwable2);

				return throwable2;
			}
		}

		Class<?> clazz = throwable.getClass();

		String className = clazz.getName();

		if (className.equals(
					"com.liferay.knowledgebase.exception.DuplicateKBFolderNameException")) {
			return new com.liferay.knowledgebase.exception.DuplicateKBFolderNameException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.InvalidKBFolderNameException")) {
			return new com.liferay.knowledgebase.exception.InvalidKBFolderNameException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBArticleContentException")) {
			return new com.liferay.knowledgebase.exception.KBArticleContentException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBArticleImportException")) {
			return new com.liferay.knowledgebase.exception.KBArticleImportException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBArticleParentException")) {
			return new com.liferay.knowledgebase.exception.KBArticleParentException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBArticlePriorityException")) {
			return new com.liferay.knowledgebase.exception.KBArticlePriorityException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBArticleSourceURLException")) {
			return new com.liferay.knowledgebase.exception.KBArticleSourceURLException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBArticleTitleException")) {
			return new com.liferay.knowledgebase.exception.KBArticleTitleException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBArticleUrlTitleException")) {
			return new com.liferay.knowledgebase.exception.KBArticleUrlTitleException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBCommentContentException")) {
			return new com.liferay.knowledgebase.exception.KBCommentContentException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBTemplateContentException")) {
			return new com.liferay.knowledgebase.exception.KBTemplateContentException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.KBTemplateTitleException")) {
			return new com.liferay.knowledgebase.exception.KBTemplateTitleException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.NoSuchKBArticleSelectorException")) {
			return new com.liferay.knowledgebase.exception.NoSuchKBArticleSelectorException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.NoSuchArticleException")) {
			return new com.liferay.knowledgebase.exception.NoSuchArticleException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.NoSuchCommentException")) {
			return new com.liferay.knowledgebase.exception.NoSuchCommentException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.NoSuchFolderException")) {
			return new com.liferay.knowledgebase.exception.NoSuchFolderException(throwable.getMessage(),
				throwable.getCause());
		}

		if (className.equals(
					"com.liferay.knowledgebase.exception.NoSuchTemplateException")) {
			return new com.liferay.knowledgebase.exception.NoSuchTemplateException(throwable.getMessage(),
				throwable.getCause());
		}

		return throwable;
	}

	public static Object translateOutputKBArticle(BaseModel<?> oldModel) {
		KBArticleClp newModel = new KBArticleClp();

		newModel.setModelAttributes(oldModel.getModelAttributes());

		newModel.setKBArticleRemoteModel(oldModel);

		return newModel;
	}

	public static Object translateOutputKBComment(BaseModel<?> oldModel) {
		KBCommentClp newModel = new KBCommentClp();

		newModel.setModelAttributes(oldModel.getModelAttributes());

		newModel.setKBCommentRemoteModel(oldModel);

		return newModel;
	}

	public static Object translateOutputKBFolder(BaseModel<?> oldModel) {
		KBFolderClp newModel = new KBFolderClp();

		newModel.setModelAttributes(oldModel.getModelAttributes());

		newModel.setKBFolderRemoteModel(oldModel);

		return newModel;
	}

	public static Object translateOutputKBTemplate(BaseModel<?> oldModel) {
		KBTemplateClp newModel = new KBTemplateClp();

		newModel.setModelAttributes(oldModel.getModelAttributes());

		newModel.setKBTemplateRemoteModel(oldModel);

		return newModel;
	}

	private static Log _log = LogFactoryUtil.getLog(ClpSerializer.class);
	private static String _servletContextName;
	private static boolean _useReflectionToTranslateThrowable = true;
}