package com.bc.ceres.glevel;

/**
 * This package allows to add multi-resolution capability to JAI.
 *
 * The framework has been designed taking into account the following requirements:
 * <ul>
 *   <li>A multi-resolution image ({@link IMultiLevelImage}) shall manage its lower-resolution instances such that
 *       the same lower-resolution image instance is returned for the same level.
 *   </li>
 *   <li>It should be possible to add the multi-resolution capability to any existing {@code RenderedImage} (see {@link MultiLevelImageImpl}).
 *   </li>
 *   <li>Classes implementing the multi-resolution capability may use any JAI {@code OpImage} DAG to produce its tiles.
 *        It should be easy to implement the multi-resolution capability (see {@link MultiLevelImageSupport}). Tile computation
 *        shall then directly take into account the resolution level.
 *   </li>
 * </ul>
 */