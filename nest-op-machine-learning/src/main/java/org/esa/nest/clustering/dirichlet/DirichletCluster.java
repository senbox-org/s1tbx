/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.clustering.dirichlet;

import com.google.common.reflect.TypeToken;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;

import org.apache.hadoop.io.Writable;
//import org.apache.mahout.clustering.Printable;
//import org.apache.mahout.clustering.dirichlet.models.Model;
import org.apache.mahout.math.Vector;

//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.reflect.TypeToken;
import java.awt.print.Printable;
import org.apache.mahout.clustering.Model;

public class DirichletCluster<O> implements Writable {

    @Override
    public void readFields(DataInput in) throws IOException {
        this.totalCount = in.readDouble();
        this.model = readModel(in);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(totalCount);
        writeModel(out, model);
    }
    private Model<O> model; // the model for this iteration
    private double totalCount; // total count of observations for the model

    public DirichletCluster(Model<O> model, double totalCount) {
        super();
        this.model = model;
        this.totalCount = totalCount;
    }

    public DirichletCluster(Model<O> model) {
        super();
        this.model = model;
        this.totalCount = 0.0;
    }

    public DirichletCluster() {
        super();
    }

    public Model<O> getModel() {
        return model;
    }

    public void setModel(Model<O> model) {
        this.model = model;
        this.totalCount += model.getTotalObservations();
    }

    public double getTotalCount() {
        return totalCount;
    }
    private static final Type clusterType = new TypeToken<DirichletCluster<Vector>>() {
    }.getType();

    /**
     * Reads a typed Model instance from the input stream
     */
    public static <O> Model<O> readModel(DataInput in) throws IOException {
        String modelClassName = in.readUTF();
        Model<O> model;
        try {
            model = Class.forName(modelClassName).asSubclass(Model.class).newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }
        model.readFields(in);
        return model;
    }

    /**
     * Writes a typed Model instance to the output stream
     */
    public static void writeModel(DataOutput out, Model<?> model) throws IOException {
        out.writeUTF(model.getClass().getName());
        model.write(out);
    }
//  @Override
//  public String asFormatString(String[] bindings) {
//    return model.toString();
//  }
//  
////  @Override
//  public String asJsonString() {
//    GsonBuilder builder = new GsonBuilder();
//    builder.registerTypeAdapter(Model.class, new JsonModelAdapter());
//    Gson gson = builder.create();
//    return gson.toJson(this, clusterType);
//  }
}