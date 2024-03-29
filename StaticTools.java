package com.koordinator.epsilon.Koordinator.Validaciones.MetacortexStaticLibrary;

import com.koordinator.epsilon.Koordinator.Excepciones.ActivoNoEncontradoException;
import com.koordinator.epsilon.Koordinator.Respuestas.RespuestaIndicadorTecnico;
import com.koordinator.epsilon.Koordinator.entidades.AssetPrice;
import com.koordinator.epsilon.Koordinator.entidades.HistoricDataWrapper;
import com.koordinator.epsilon.Koordinator.entidades.TechnicalIndicatorWrapper;
import com.koordinator.epsilon.Koordinator.entidades.TechnicalRegistry;
import com.koordinator.epsilon.Koordinator.repositorio.RepositorioActivos;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;


public class StaticTools {


    public static int buscarIntervalo(ArrayList<HistoricDataWrapper> lista, String intervalo) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getPeriod().equals(intervalo)) {
                return i;
            }
        }
        return -1;
    }

    public static int buscarIndicador(ArrayList<TechnicalIndicatorWrapper> indicadorTecnicos, String indicadorBuscado, int intervalo, String periodoDatosHistoricos, String tipoSeries) {
        for (int i = 0; i < indicadorTecnicos.size(); i++) {
            TechnicalIndicatorWrapper indicadorTecnico = indicadorTecnicos.get(i);
            if (indicadorTecnico.getIndicatorName().equalsIgnoreCase(indicadorBuscado) &&
                    indicadorTecnico.getInterval() == intervalo &&
                    indicadorTecnico.getHistoricPeriod().equalsIgnoreCase(periodoDatosHistoricos) &&
                    indicadorTecnico.getSeriesType().equalsIgnoreCase(tipoSeries)) {
                return i;
            }
        }
        return -1;
    }

    public static int buscarIndicadorSimple(ArrayList<TechnicalIndicatorWrapper> indicadorTecnicos, String indicadorBuscado, String periodoHistorico) {
        for (int i = 0; i < indicadorTecnicos.size(); i++) {
            TechnicalIndicatorWrapper indicadorTecnico = indicadorTecnicos.get(i);
            if (indicadorTecnico.getIndicatorName().equalsIgnoreCase(indicadorBuscado) && indicadorTecnico.getHistoricPeriod().equalsIgnoreCase(periodoHistorico)) {
                return i;
            }
        }
        return -1;
    }

    public static int buscarIndicadorSimpleConSeries(ArrayList<TechnicalIndicatorWrapper> indicadorTecnicos, String indicadorBuscado, String periodoHistorico, String seriesType) {
        for (int i = 0; i < indicadorTecnicos.size(); i++) {
            TechnicalIndicatorWrapper indicadorTecnico = indicadorTecnicos.get(i);
            if (indicadorTecnico.getIndicatorName().equalsIgnoreCase(indicadorBuscado) && indicadorTecnico.getHistoricPeriod().equalsIgnoreCase(periodoHistorico) && indicadorTecnico.getSeriesType().equalsIgnoreCase(seriesType)) {
                return i;
            }
        }
        return -1;
    }

    public static String getTechnicalIndicatorURL(String technicalIndicator, Map<String, String> queryParameters) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ValidacionesEstaticas.URLLOCAL);
        stringBuilder.append(ValidacionesEstaticas.endPointTEchnical);
        stringBuilder.append(technicalIndicator + "?");
        stringBuilder.append(ValidacionesEstaticas.nombreParBase + "=" + queryParameters.get(ValidacionesEstaticas.nombreParBase));
        stringBuilder.append("&");
        stringBuilder.append(ValidacionesEstaticas.nombreParContra + "=" + queryParameters.get(ValidacionesEstaticas.nombreParContra));
        if (queryParameters.get(ValidacionesEstaticas.intervaloHistorico) != null) {
            stringBuilder.append("&");
            stringBuilder.append(ValidacionesEstaticas.intervaloHistorico + "=" + queryParameters.get(ValidacionesEstaticas.intervaloHistorico));
        }
        if (queryParameters.get(ValidacionesEstaticas.intervaloPeriodoIndicador) != null) {
            stringBuilder.append("&");
            stringBuilder.append(ValidacionesEstaticas.intervaloPeriodoIndicador + "=" + queryParameters.get(ValidacionesEstaticas.intervaloPeriodoIndicador));
        }
        if (queryParameters.get(ValidacionesEstaticas.tipoSeriesIndicador) != null) {
            stringBuilder.append("&");
            stringBuilder.append(ValidacionesEstaticas.tipoSeriesIndicador + "=" + queryParameters.get(ValidacionesEstaticas.tipoSeriesIndicador));
        }
        return stringBuilder.toString();
    }

    public static TechnicalRegistry[][] recuperarIndicador(String nombre, Map<String, String> queryParameters, RepositorioActivos repositorioActivos, String tipo) {
       if (ValidacionesEstaticas.validacionGENERAL(queryParameters,tipo)) {
            Optional<AssetPrice> precioMONGO = repositorioActivos.findById(queryParameters.get(ValidacionesEstaticas.nombreParBase) + queryParameters.get(ValidacionesEstaticas.nombreParContra));
            if (precioMONGO.isPresent()) {
                int resBusqueda = busquedaIndicadorGeneral(precioMONGO.get(), nombre, tipo, queryParameters);
                if (resBusqueda != -1) {
                    return precioMONGO.get().getIndicatorList().get(resBusqueda).getRawTechnicalData();
                } else {
                   RespuestaIndicadorTecnico obj= new RestTemplate().getForObject(StaticTools.getTechnicalIndicatorURL(nombre.toLowerCase(), queryParameters), RespuestaIndicadorTecnico.class);
                    if (obj.getEstado() != 200) throw new ActivoNoEncontradoException(obj.getMensaje());
                    return obj.getListaTecnico();
                }
            } else {
                RespuestaIndicadorTecnico obj = new RestTemplate().getForObject(StaticTools.getTechnicalIndicatorURL(nombre.toLowerCase(), queryParameters), RespuestaIndicadorTecnico.class);
                if (obj.getEstado() != 200) throw new ActivoNoEncontradoException(obj.getMensaje());
                return obj.getListaTecnico();
            }

        } else {
            throw new ActivoNoEncontradoException("Incorrect parameters were introduced!");
        }
    }

    private static int busquedaIndicadorGeneral(AssetPrice precioMongo, String nombre, String tipo, Map<String, String> queryParameters) {
        switch (tipo.toLowerCase()) {
            case "general":
                return StaticTools.buscarIndicador(precioMongo.getIndicatorList(),
                        nombre.toLowerCase(),
                        Integer.parseInt(queryParameters.get(ValidacionesEstaticas.intervaloPeriodoIndicador)),
                        queryParameters.get(ValidacionesEstaticas.intervaloHistorico),
                        queryParameters.get(ValidacionesEstaticas.tipoSeriesIndicador));
            case "simplesinseries":
                return StaticTools.buscarIndicadorSimple(precioMongo.getIndicatorList(), nombre.toLowerCase(), queryParameters.get(ValidacionesEstaticas.intervaloHistorico));
            case "simpleconseries":
                return StaticTools.buscarIndicadorSimpleConSeries(precioMongo.getIndicatorList(),nombre.toLowerCase(),queryParameters.get(ValidacionesEstaticas.intervaloHistorico),queryParameters.get(ValidacionesEstaticas.tipoSeriesIndicador));
            default:
                return -1;
        }
    }
}
